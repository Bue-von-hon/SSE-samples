package com.example.youthcon;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.errorprone.annotations.concurrent.GuardedBy;

@Component
@Slf4j
public class SseEmitters {

    // todo(hun): 아티클별 조회수
    private static final Map<String, AtomicLong> articleId2AtomicLong = new ConcurrentHashMap<>();

    @GuardedBy("lock")
    private final Cache<String, Connection> tabIdToConnection = CacheBuilder.newBuilder()
                                                                            .expireAfterAccess(10000L, TimeUnit.MILLISECONDS)
                                                                            .build();

    @GuardedBy("lock")
    private final Map<String, Set<Connection>> articleToConnection = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Connection connect(final String tabId, final String articleId) {
        final Connection oldConnection = tabIdToConnection.getIfPresent(tabId);

        if (oldConnection != null) {
            oldConnection.complete();
        }

        final Connection newConnection = new Connection(tabId, 10000L);
        setCallback(articleId, newConnection);

        lock.lock();
        try {
            tabIdToConnection.put(tabId, newConnection);
            final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
            connections.add(newConnection);
            articleToConnection.put(articleId, connections);
            log.info("emitter list size: {}", tabIdToConnection.size());
            log.info("emitter : {}", connections);
        } finally {
            lock.unlock();
        }

        return newConnection;
    }

    private void setCallback(final String articleId, final Connection connection) {
        connection.onCompletion(() -> deleteConnectionFromArticleToConnection(articleId, connection));
        connection.onError(error -> deleteConnectionFromArticleToConnection(articleId, connection));
        connection.onTimeout(() -> deleteConnectionFromArticleToConnection(articleId, connection));
    }

    private void deleteConnectionFromArticleToConnection(String articleId, Connection connection) {
        lock.lock();
        try {
            final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
            if (!connections.isEmpty()) {
                connections.remove(connection);
                articleToConnection.put(articleId, connections);
                tabIdToConnection.cleanUp();
            }

        } finally {
            lock.unlock();
        }
    }

    public void count(final String articleId) {
        final AtomicLong counter = articleId2AtomicLong.computeIfAbsent(articleId, k -> new AtomicLong());
        final long count = counter.incrementAndGet();
        articleId2AtomicLong.put(articleId, counter);

        final Set<Connection> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        connections.parallelStream().forEach(connection -> connection.count(count));
    }
}
