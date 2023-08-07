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

@Service
@Slf4j
public class CommentService {

    // todo(hun): 아티클별 코멘트
    private static final Map<String, List<Comment>> articleId2Comments = new ConcurrentHashMap<>();

    private final Cache<String, Connection> tabIdToConnection = CacheBuilder.newBuilder()
                                                                            .expireAfterAccess(10000L, TimeUnit.MILLISECONDS)
                                                                            .build();

    private final Map<String, Set<Connection>> articleToConnection = new ConcurrentHashMap<>();

    public synchronized Connection startViewingArticle(final String tabId, final String articleId) {
        completeOldConnection(tabId);
        final Connection newConnection = getNewConnection(tabId, articleId);
        updateAssociateTabAndArticleWithConnection(tabId, articleId, newConnection);
        return newConnection;
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

    private void completeOldConnection(final String tabId) {
        final Connection oldConnection = tabIdToConnection.getIfPresent(tabId);

        if (oldConnection != null) {
            oldConnection.complete();
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

    public synchronized void saveAndSend(
                                        final Comment comment,
                                        final String articleId,
                                        final String tabId) {
        updateComments(comment, tabId);
        sendComment(comment, articleId);
    }

    private void updateComments(final Comment comment, final String tabId) {
        final List<Comment> comments = articleId2Comments.computeIfAbsent(tabId, k -> new ArrayList<>());
        comments.add(comment);
        articleId2Comments.put(tabId, comments);
    }

    private void sendComment(final Comment comment, final String articleId) {
        final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        connections.parallelStream().forEach(connection -> connection.sendComment(comment));
    }

}
