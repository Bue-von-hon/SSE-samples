package com.example.youthcon;

import java.io.IOException;
import java.util.function.Consumer;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.google.common.base.Objects;

import lombok.Getter;

public class Connection {

    @Getter
    private final SseEmitter emitter;

    private final String id;

    public Connection(final String id, final Long timeout) {
        this.emitter = new SseEmitter(timeout);
        this.id = id;
    }

    public synchronized void onCompletion(final Runnable callback) {
        emitter.onCompletion(callback);
    }

    public synchronized void onError(final Consumer<Throwable> callback) {
        emitter.onError(callback);
    }

    public synchronized void onTimeout(final Runnable callback) {
        emitter.onTimeout(callback);
    }

    public synchronized void complete() {
        emitter.complete();
    }

    public synchronized void connect() {
        final SseEmitter.SseEventBuilder eventBuilder =
                SseEmitter.event()
                          .name("connect")
                          .data("connected!")
                          .reconnectTime(1L);
        send(eventBuilder);
    }

    public synchronized void count(final long count) {
        final SseEmitter.SseEventBuilder eventBuilder =
                SseEmitter.event()
                          .name("count")
                          .data(count)
                          .reconnectTime(1L);
        send(eventBuilder);
    }

    private synchronized void send(final SseEventBuilder builder) {
        try {
            emitter.send(builder);
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
