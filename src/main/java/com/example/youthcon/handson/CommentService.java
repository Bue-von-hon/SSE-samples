package com.example.youthcon.handson;

import java.util.Set;

import com.example.youthcon.preparation.*;

import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private final ArticleIdToCommentsManager articleIdToCommentsManager;

    private final TabIdToConnectionManager tabIdToConnectionManager;

    private final ArticleToConnectionManager articleToConnectionManager;

    public CommentService(
            ArticleIdToCommentsManager articleIdToCommentsManager,
            TabIdToConnectionManager tabIdToConnectionManager,
            ArticleToConnectionManager articleToConnectionManager) {
        this.articleIdToCommentsManager = articleIdToCommentsManager;
        this.tabIdToConnectionManager = tabIdToConnectionManager;
        this.articleToConnectionManager = articleToConnectionManager;
    }

    // 특정 탭에서 특정한 아티클을 보기 시작할때 호출되는 메소드
    public synchronized Connection startViewingArticle(final String tabId, final String articleId) {
        tabIdToConnectionManager.completeOldConnection(tabId);
        final Connection newConnection = getNewConnection(tabId, articleId);
        updateAssociateTabAndArticleWithConnection(tabId, articleId, newConnection);
        return newConnection;
    }

    private Connection getNewConnection(final String tabId, final String articleId) {
        final Connection newConnection = new Connection(tabId, 300000L);
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
        final Set<Connection> connections = articleToConnectionManager.getOrDefault(articleId);
        if (!connections.isEmpty()) {
            connections.remove(connection);
            articleToConnectionManager.put(articleId, connections);
            tabIdToConnectionManager.cleanUp();
        }
    }

    private void updateAssociateTabAndArticleWithConnection(
            final String tabId,
            final String articleId,
            final Connection newConnection) {
        tabIdToConnectionManager.updateTabIdToConnection(tabId, newConnection);
        articleToConnectionManager.updateArticleToConnection(articleId, newConnection);
    }

    // 댓글을 저장하고 연결된 커넥션들에게 댓글 생성 이벤트를 전달한다.
    public synchronized void saveAndSend(
            final Comment comment,
            final String articleId,
            final String tabId) {
        articleIdToCommentsManager.updateComments(comment, articleId);
        sendComment(comment, articleId, tabId);
    }

    private void sendComment(final Comment comment, final String articleId, final String tabId) {
        final Connection selfConnection = tabIdToConnectionManager.getIfPresent(tabId);
        final Set<Connection> connections = articleToConnectionManager.getOrDefault(articleId);
        
        connections.stream()
                .filter(connection -> !connection.equals(selfConnection))
                .forEach(connection -> connection.sendComment(comment));
    }
}
