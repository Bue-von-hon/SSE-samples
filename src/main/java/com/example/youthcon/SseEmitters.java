package com.example.youthcon;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
@Slf4j
public class SseEmitters {

    // todo(hun): 아티클별 조회수
    private static final Map<String, AtomicLong> articleId2AtomicLong = new ConcurrentHashMap<>();

    private final LoadingCache<String, SseEmitter> tabId2SseEmitter = CacheBuilder.newBuilder()
                                                                    .expireAfterAccess(10000L, TimeUnit.MILLISECONDS)
                                                                    .build(
                                                                            new CacheLoader<String, SseEmitter>() {
                                                                                @Override
                                                                                public SseEmitter load(String key) {
                                                                                    // 캐시 항목이 없거나 만료된 경우에 값을 로드하는 코드
                                                                                    return null;
                                                                                }
                                                                            }
                                                                    );

    private final Map<String, Set<SseEmitter>> articleToEmitter = new ConcurrentHashMap<>();

    public SseEmitter connect(final String tabId, final String articleId) {
        final SseEmitter oldEmitter = tabId2SseEmitter.getIfPresent(tabId);

        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        final SseEmitter newEmitter = new SseEmitter(10000L);
        setCallback(articleId, newEmitter);
        tabId2SseEmitter.put(tabId, newEmitter);

        final Set<SseEmitter> connections = articleToEmitter.getOrDefault(articleId, new HashSet<>());
        connections.add(newEmitter);
        articleToEmitter.put(articleId, connections);

        log.info("emitter list size: {}", tabId2SseEmitter.size());
        log.info("emitter : {}", newEmitter);
        return newEmitter;
    }

    private void setCallback(final String articleId, final SseEmitter emitter) {
        emitter.onCompletion(() -> {
            synchronized (articleToEmitter) {
                final Set<SseEmitter> emitters = articleToEmitter.get(articleId);
                if (emitters != null && !emitters.isEmpty()) {
                    emitters.remove(emitter);
                    articleToEmitter.put(articleId, emitters);
                }
            }
        });

        emitter.onError(error -> {
            synchronized (articleToEmitter) {
                final Set<SseEmitter> emitters = articleToEmitter.get(articleId);
                if (emitters != null && !emitters.isEmpty()) {
                    emitters.remove(emitter);
                    articleToEmitter.put(articleId, emitters);
                }
            }
        });

        emitter.onTimeout(() -> {
            synchronized (articleToEmitter) {
                final Set<SseEmitter> emitters = articleToEmitter.get(articleId);
                if (emitters != null && !emitters.isEmpty()) {
                    emitters.remove(emitter);
                    articleToEmitter.put(articleId, emitters);
                }
            }
        });
    }

    public void count(final String articleId) {
        final AtomicLong counter = articleId2AtomicLong.computeIfAbsent(articleId, k -> new AtomicLong());
        final long count = counter.incrementAndGet();
        articleId2AtomicLong.put(articleId, counter);

        final Set<SseEmitter> emitters = articleToEmitter.getOrDefault(articleId, new HashSet<>());
        emitters.forEach(emitter -> {
            try {
                final SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                                                                          .name("count")
                                                                          .data(count)
                                                                          .reconnectTime(10000L);
                emitter.send(eventBuilder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // 아티클을 보고 있던 탭에서 다른 아티클로 넘어갔다는 요청이 전송된 경우
    public void changeTaticle(final String tabId, final String newArticleId, final String oldArticleId) {
        final SseEmitter oldEmitter = tabId2SseEmitter.getIfPresent(tabId);
        if (oldEmitter == null) {
            return;
        }

        final Set<SseEmitter> oldEmitters = articleToEmitter.getOrDefault(oldArticleId, new HashSet<>());
        oldEmitters.remove(oldEmitter);
        articleToEmitter.put(oldArticleId, oldEmitters);

        final Set<SseEmitter> newEmitters = articleToEmitter.getOrDefault(newArticleId, new HashSet<>());
        newEmitters.add(oldEmitter);
        articleToEmitter.put(newArticleId, newEmitters);

        log.info("tabId : {}", tabId);
        log.info("previousArticleId : {}", oldArticleId);
        log.info("articleId : {}", newArticleId);
    }

    public void disconnect(final String tabId, final String articleId) {
        cleanup(tabId, articleId);
    }

    private void cleanup(String tabId, String articleId) {
        final SseEmitter oldEmitter = tabId2SseEmitter.getIfPresent(tabId);
        final Set<SseEmitter> emitters = articleToEmitter.get(articleId);
        emitters.remove(oldEmitter);
        articleToEmitter.put(articleId, emitters);
        // 이전에 생성된 커넥션 완료 처리
        if (oldEmitter != null) {
            oldEmitter.complete();
            tabId2SseEmitter.invalidate(tabId);
        }
    }
}