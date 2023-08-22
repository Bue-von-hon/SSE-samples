package com.example.youthcon.preparation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ArticleToConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ArticleToConnectionManager.class);

    // 아티클과 연결된 커넥션, 같은 아티클을 보는 여러 탭에 이벤트 전송을 하기위함
    private final Map<String, Set<Connection>> articleToConnection = new ConcurrentHashMap<>();

    public Set<Connection> getOrDefault(final String articleId) {
        return articleToConnection.getOrDefault(articleId, new HashSet<>());
    }

    public void put(final String articleId, final Set<Connection> connections) {
        articleToConnection.put(articleId, connections);
    }

    public void updateArticleToConnection(final String articleId, final Connection newConnection) {
        final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        connections.add(newConnection);
        articleToConnection.put(articleId, connections);

        log.info("emitter : {}", connections);
    }

    public boolean removeConnectionIfNotEmpty(String articleId, Connection connection) {
        final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        boolean notEmpty = !connections.isEmpty();
        if (notEmpty) {
            connections.remove(connection);
            articleToConnection.put(articleId, connections);
        }
        return notEmpty;
    }

}
