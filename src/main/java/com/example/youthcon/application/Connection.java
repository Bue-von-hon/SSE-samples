package com.example.youthcon.application;

import java.io.IOException;
import java.util.function.Consumer;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.example.youthcon.Comment;
import com.google.common.base.Objects;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Connection {

    @Getter
    private final SseEmitter emitter;

    private final String id;

    public Connection(final String id, final Long timeout) {
        this.emitter = new SseEmitter(timeout);
        this.id = id;
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
                          .reconnectTime(1L);
        send(eventBuilder);
    }

    public void sendComment(final Comment comment) {
        final SseEmitter.SseEventBuilder eventBuilder =
                SseEmitter.event()
                          .name("newComment")
                          .data(comment)
                          .reconnectTime(1L);
        send(eventBuilder);
    }

    private void send(final SseEventBuilder builder) {
        try {
            emitter.send(builder);
            log.info("comment sent to client");  
        } catch (IOException e) {
            throw new RuntimeException(e);
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
