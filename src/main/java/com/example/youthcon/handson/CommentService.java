package com.example.youthcon.handson;

import java.util.Set;

import com.example.youthcon.preparation.*;

import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private final ArticleIdToCommentsManager articleIdToCommentsManager;

    private final ConnectionManager connectionManager;

    public CommentService(
            ArticleIdToCommentsManager articleIdToCommentsManager,
            ConnectionManager connectionManager) {
        this.articleIdToCommentsManager = articleIdToCommentsManager;
        this.connectionManager = connectionManager;
    }

    // 특정 탭에서 특정한 아티클을 보기 시작할때 호출되는 메소드
    public synchronized Connection startViewingArticle(final String tabId, final String articleId) {
        connectionManager.completeOldConnection(tabId);
        final Connection newConnection = connectionManager.getNewConnection(tabId, articleId);
        newConnection.connect();
        connectionManager.updateTabIdToConnection(tabId, newConnection);
        connectionManager.updateArticleToConnection(articleId, newConnection);
        return newConnection;
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
        final Connection selfConnection = connectionManager.getSelfConnection(tabId);
        final Set<Connection> connections = connectionManager.getConnections(articleId);
        
        connections.stream()
                .filter(connection -> !connection.equals(selfConnection))
                .forEach(connection -> connection.sendComment(comment));
    }
}
