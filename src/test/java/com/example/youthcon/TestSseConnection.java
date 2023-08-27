package com.example.youthcon;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

class TestSseConnection {

    private final CountDownLatch open = new CountDownLatch(1);
    private final CountDownLatch close = new CountDownLatch(1);
    private final CountDownLatch failure = new CountDownLatch(1);
    private final List<String> receivedData = new ArrayList<>();
    private final CountDownLatch eventCountLatch;
    private final EventSourceListener listener = new EventSourceListener() {
        @Override
        public void onClosed(@NotNull EventSource eventSource) {
            close.countDown();
        }

        @Override
        public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
            receivedData.add(data);
            eventCountLatch.countDown();
        }

        @Override
        public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
            failure.countDown();
        }

        @Override
        public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
            open.countDown();
        }
    };

    public TestSseConnection(int eventCount) {
        this.eventCountLatch = new CountDownLatch(eventCount);
    }

    public EventSourceListener listener() {
        return listener;
    }

    public boolean isOpened() {
        await(open);
        return true;
    }

    public boolean isClosed() {
        await(close);
        return true;
    }

    public List<String> getReceivedData() {
        await(eventCountLatch);
        return receivedData;
    }

    private void await(CountDownLatch latch) {
        try {
            boolean isTimeout = !latch.await(2, TimeUnit.SECONDS);
            if (isTimeout) {
                throw new RuntimeException("Timeout");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
