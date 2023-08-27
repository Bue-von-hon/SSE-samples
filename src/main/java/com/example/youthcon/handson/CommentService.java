package com.example.youthcon.handson;

import com.example.youthcon.preparation.Comment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class CommentService {

    private final long sseTimeout;
    private final long sseReconnectTime;
    private final Map<String, Set<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    public CommentService(@Value("${sse.timeout}") long sseTimeout, @Value("${sse.reconnect-time}") long sseReconnectTime) {
        this.sseTimeout = sseTimeout;
        this.sseReconnectTime = sseReconnectTime;
    }

    public SseEmitter startViewingArticle(String articleId) {
        SseEmitter sseEmitter = createSseEmitter(articleId);
        registerSseEmitter(articleId, sseEmitter);
        send(sseEmitter, eventBuilder("connect", "connected!"));
        return sseEmitter;
    }

    private SseEmitter createSseEmitter(String articleId) {
        SseEmitter sseEmitter = new SseEmitter(sseTimeout);
        Runnable unregisterEmitter = () -> {
            Set<SseEmitter> sseEmitters = emitterMap.get(articleId);
            sseEmitters.remove(sseEmitter);
        };
        sseEmitter.onCompletion(unregisterEmitter);
        sseEmitter.onTimeout(unregisterEmitter);
        sseEmitter.onError(e -> unregisterEmitter.run());
        return sseEmitter;
    }

    private void registerSseEmitter(String articleId, SseEmitter sseEmitter) {
        emitterMap.putIfAbsent(articleId, new CopyOnWriteArraySet<>());
        Set<SseEmitter> emitters = emitterMap.get(articleId);
        emitters.add(sseEmitter);
    }

    private SseEmitter.SseEventBuilder eventBuilder(String eventName, Object data) {
        return SseEmitter.event()
                .name(eventName)
                .data(data)
                .reconnectTime(sseReconnectTime);
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder eventBuilder) {
        try {
            emitter.send(eventBuilder);
        } catch (IOException e) {
            emitter.complete();
        }
    }

    public void saveComment(String articleId, Comment comment) {
        if (emitterMap.containsKey(articleId)) {
            Set<SseEmitter> emitters = emitterMap.get(articleId);
            SseEmitter.SseEventBuilder eventBuilder = eventBuilder("newComment", comment);
            emitters.forEach(connection -> send(connection, eventBuilder));
        }
    }
}
