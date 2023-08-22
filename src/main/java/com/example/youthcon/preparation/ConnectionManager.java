package com.example.youthcon.preparation;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ConnectionManager {

    private final TabIdToConnectionManager tabIdToConnectionManager;

    private final ArticleToConnectionManager articleToConnectionManager;

    public ConnectionManager(TabIdToConnectionManager tabIdToConnectionManager, ArticleToConnectionManager articleToConnectionManager) {
        this.tabIdToConnectionManager = tabIdToConnectionManager;
        this.articleToConnectionManager = articleToConnectionManager;
    }

    public void completeOldConnection(final String tabId) {
        tabIdToConnectionManager.completeOldConnection(tabId);
    }

    public Connection getNewConnection(final String tabId, final String articleId) {
        final Connection newConnection = new Connection(tabId, 300000L);
        setCallback(articleId, newConnection);
        return newConnection;
    }

    private void setCallback(final String articleId, final Connection connection) {
        connection.onCompletion(() -> removeConnectionAndUpdateTab(articleId, connection));
        connection.onError(error -> removeConnectionAndUpdateTab(articleId, connection));
        connection.onTimeout(() -> removeConnectionAndUpdateTab(articleId, connection));
    }

    private void removeConnectionAndUpdateTab(String articleId, Connection connection) {
        if (articleToConnectionManager.removeConnectionIfNotEmpty(articleId, connection)) {
            tabIdToConnectionManager.cleanUp();
        }
    }

    public void updateTabIdToConnection(final String tabId, final Connection newConnection) {
        tabIdToConnectionManager.updateTabIdToConnection(tabId, newConnection);
    }

    public void updateArticleToConnection(final String articleId, final Connection newConnection) {
        articleToConnectionManager.updateArticleToConnection(articleId, newConnection);
    }

    public Connection getSelfConnection(final String tabId) {
        return tabIdToConnectionManager.getIfPresent(tabId);
    }

    public Set<Connection> getConnections(final String articleId) {
        return articleToConnectionManager.getOrDefault(articleId);
    }
}
