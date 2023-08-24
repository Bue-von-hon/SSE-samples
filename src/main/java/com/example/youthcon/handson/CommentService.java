package com.example.youthcon.handson;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.youthcon.preparation.*;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CommentService {

    private final Map<String, Set<SseEmitter>> articleToConnection = new ConcurrentHashMap<>();

    public SseEmitter startViewingArticle(String articleId) {
        SseEmitter sseEmitter = new SseEmitter(300000L);
        final SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name("connect")
                .data("connected!")
                .reconnectTime(3000L);
        send(eventBuilder, sseEmitter);

        final Set<SseEmitter> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        connections.add(sseEmitter);
        articleToConnection.put(articleId, connections);
        return sseEmitter;
    }

    public void saveAndSend(Comment comment, String articleId) {
        Set<SseEmitter> connections = articleToConnection.getOrDefault(articleId, new HashSet<>());
        final SseEmitter.SseEventBuilder eventBuilder =
                SseEmitter.event()
                        .name("newComment")
                        .data(comment)
                        .reconnectTime(30000L);
        connections.stream()
                .forEach(connection -> {
                    send(eventBuilder, connection);
                });
    }

    void send(final SseEmitter.SseEventBuilder builder, SseEmitter emitter) {
        try {
            emitter.send(builder);
        } catch (IOException e) {
            // 유저가 탭을 닫고, 충분한 시간이 지나기 전에 댓글을 전송해야한다면, broken pipe 에러가 날 수 있다
            // 그래서 complete 해주어야 한다.
            emitter.complete();
        }
    }
}
