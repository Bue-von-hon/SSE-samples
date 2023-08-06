package com.example.youthcon;

import static java.lang.Thread.sleep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SseEmitterTest {

    @LocalServerPort
    private int port;

    private static final OkHttpClient client = new OkHttpClient();

    private static final EventSource.Factory factory = EventSources.createFactory(client);

    @Test
    void testConnectToSseEmitter() throws InterruptedException {
        final String[] receivedData = new String[1];

        final EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {}

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                receivedData[0] = data;
            }

            @Override
            public void onClosed(EventSource eventSource) {}

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {}
        };

        final Request request = new Request.Builder().url("http://localhost:" + port + "/connect?tabId=1&articleId=1").build();
        factory.newEventSource(request, listener);

        // wait for the connection and the first event
        sleep(2000);

        Assertions.assertEquals("connected!", receivedData[0]);
    }

}
