package com.example.youthcon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Slf4j
public class SseEmitters {

    // todo(hun): 아티클별 조회수
    private static final ConcurrentHashMap<String, AtomicLong> articleId2AtomicLong = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, SseEmitter> tabId2SseEmitter = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<String>> articleId2tabId = new ConcurrentHashMap<>();

    SseEmitter add(final String tabId, final String articleId) {
        final SseEmitter emitter = new SseEmitter();
        articleId2tabId.computeIfAbsent(articleId, k -> new ArrayList<>()).add(tabId);
        tabId2SseEmitter.putIfAbsent(tabId, emitter);

        final SseEmitter emitters = tabId2SseEmitter.get(tabId);
        log.info("new emitter added: {}", emitter);
        log.info("emitter list size: {}", tabId2SseEmitter.size());
        log.info("emitter list: {}", emitters);
        emitter.onCompletion(() -> {
            log.info("onCompletion callback");
            tabId2SseEmitter.remove(tabId);
        });
        emitter.onTimeout(() -> {
            log.info("onTimeout callback");
            emitter.complete();
        });

        return emitter;
    }

    public void count(final String articleId) {
        final AtomicLong counter = articleId2AtomicLong.computeIfAbsent(articleId, k -> new AtomicLong());
        final long count = counter.incrementAndGet();
        articleId2AtomicLong.put(articleId, counter);

        final List<String> tabIdList = articleId2tabId.get(articleId);
        tabIdList.forEach(tabId -> {
            final SseEmitter emitter = tabId2SseEmitter.get(tabId);
            try {
                emitter.send(SseEmitter.event()
                                       .name("count")
                                       .data(count));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}