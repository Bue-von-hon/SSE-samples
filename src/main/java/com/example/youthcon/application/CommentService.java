package com.example.youthcon.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.youthcon.Comment;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CommentService {

    // 아티클별 댓글
    private static final Map<String, List<Comment>> articleIdToComments = new ConcurrentHashMap<>();

    // 탭과 연결된 커넥션 그리고 캐시를 이용한 만료처리
    // 타임 아웃시간이 지나도 탭을 커넥션을 들고있으면 안돼는데, 만료 시간을 설정하면 굳이 지우는 연산을 안해도된다.
    private final Cache<String, Connection> tabIdToConnection = CacheBuilder.newBuilder()
            .expireAfterAccess(10000L, TimeUnit.MILLISECONDS)
            .build();

    // 아티클과 연결된 커넥션, 같은 아티클을 보는 여러 탭에 이벤트 전송을 하기위함
    private final Map<String, Set<Connection>> articleToConnection = new ConcurrentHashMap<>();

    // 특정 탭에서 특정한 아티클을 보기 시작할때 호출되는 메소드
    public synchronized Connection startViewingArticle(final String tabId, final String articleId) {
        completeOldConnection(tabId);
        final Connection newConnection = getNewConnection(tabId, articleId);
        updateAssociateTabAndArticleWithConnection(tabId, articleId, newConnection);
        return newConnection;
    }

    private void completeOldConnection(final String tabId) {
        final Connection oldConnection = tabIdToConnection.getIfPresent(tabId);

        if (oldConnection != null) {
            oldConnection.complete();
        }
    }

    private Connection getNewConnection(final String tabId, final String articleId) {
        final Connection newConnection = new Connection(tabId, 10000L);
        setCallback(articleId, newConnection);
        return newConnection;
    }

    private void setCallback(final String articleId, final Connection connection) {
        connection.onCompletion(() -> deleteConnectionFromArticleToConnection(articleId, connection));
        connection.onError(error -> deleteConnectionFromArticleToConnection(articleId, connection));
        connection.onTimeout(() -> deleteConnectionFromArticleToConnection(articleId, connection));
    }

    private void deleteConnectionFromArticleToConnection(
            final String articleId,
            final Connection connection) {
        final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        if (!connections.isEmpty()) {
            connections.remove(connection);
            articleToConnection.put(articleId, connections);
            tabIdToConnection.cleanUp();
        }
    }

    private void updateAssociateTabAndArticleWithConnection(
            final String tabId,
            final String articleId,
            final Connection newConnection) {
        updateTabIdToConnection(tabId, newConnection);
        updateArticleToConnection(articleId, newConnection);
    }

    private void updateTabIdToConnection(final String tabId, final Connection newConnection) {
        tabIdToConnection.put(tabId, newConnection);
        log.info("emitter list size: {}", tabIdToConnection.size());
    }

    private void updateArticleToConnection(final String articleId, final Connection newConnection) {
        final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        connections.add(newConnection);
        articleToConnection.put(articleId, connections);

        log.info("emitter : {}", connections);
    }

    // 댓글을 저장하고 연결된 커넥션들에게 댓글 생성 이벤트를 전달한다.
    public synchronized void saveAndSend(
            final Comment comment,
            final String articleId,
            final String tabId) {
        updateComments(comment, articleId);
        sendComment(comment, articleId, tabId);
    }

    private void updateComments(final Comment comment, final String articleId) {
        final List<Comment> comments = articleIdToComments.computeIfAbsent(articleId, k -> new ArrayList<>());
        comments.add(comment);
        articleIdToComments.put(articleId, comments);
    }

    private void sendComment(final Comment comment, final String articleId, final String tabId) {
    Connection selfConnection = tabIdToConnection.getIfPresent(tabId);
    final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
    
    connections.stream()
               .filter(connection -> !connection.equals(selfConnection))
               .forEach(connection -> connection.sendComment(comment));
    }

    public List<Comment> getAll(final String articleId) {
        return articleIdToComments.get(articleId);
    }

}
