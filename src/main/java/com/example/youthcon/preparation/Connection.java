package com.example.youthcon.preparation;

import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.google.common.base.Objects;

public class Connection {

    private static final Logger log = LoggerFactory.getLogger(Connection.class);

    private final SseEmitter emitter;

    private final String id;

    public Connection(final String id, final Long timeout) {
        this.emitter = new SseEmitter(timeout);
        this.id = id;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    public void onCompletion(final Runnable callback) {
        emitter.onCompletion(callback);
    }

    public void onError(final Consumer<Throwable> callback) {
        emitter.onError(callback);
    }

    public void onTimeout(final Runnable callback) {
        emitter.onTimeout(callback);
    }

    public void complete() {
        emitter.complete();
    }

    public void connect() {
        final SseEmitter.SseEventBuilder eventBuilder =
                SseEmitter.event()
                          .name("connect")
                          .data("connected!")
                          .reconnectTime(3000L);
        send(eventBuilder);
    }

    public void sendComment(final Comment comment) {
        final SseEmitter.SseEventBuilder eventBuilder =
                SseEmitter.event()
                          .name("newComment")
                          .data(comment)
                          .reconnectTime(30000L);
        send(eventBuilder);
    }

    private void send(final SseEventBuilder builder) {
        try {
            emitter.send(builder);
            log.info("comment sent to client");  
        } catch (IOException e) {
            // 유저가 탭을 닫고, 충분한 시간이 지나기 전에 댓글을 전송해야한다면, broken pipe 에러가 날 수 있다
            // 그래서 complete 해주어야 한다.
            emitter.complete();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        final Connection that = (Connection) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
