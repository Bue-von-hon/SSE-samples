package com.example.youthcon;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommentTest {

    @LocalServerPort
    private int port;

    private static final OkHttpClient client = new OkHttpClient();

    private static final EventSource.Factory factory = EventSources.createFactory(client);

    @Test
    void testConnectToSseEmitter() throws InterruptedException {
        final EventSourceWrapper eventWrapper = new EventSourceWrapper();
        final Request request = getConnectRequest();
        factory.newEventSource(request, eventWrapper.listener);

        // wait for the connection and the first event
        sleep(2000);

        Assertions.assertEquals("connected!", eventWrapper.receivedData.get(0));
    }


    private Request getConnectRequest() {
        return new Request.Builder().url("http://localhost:" + port + "/connect?tabId=1&articleId=1").build();
    }

    
    @Test
    void shouldSaveAndSendCommentToConnectedTabs() throws InterruptedException, IOException, JSONException {
        String articleId = "articleId";
        String tabId = "tabId";
        Comment comment = new Comment("Test content");
        EventSourceWrapper eventSourceWrapper = new EventSourceWrapper();

        // 탭에 연결
        final Request connectRequest = new Request.Builder()
                .url("http://localhost:" + port + "/connect?tabId=" + tabId + "&articleId=" + articleId)
                .build();
        factory.newEventSource(connectRequest, eventSourceWrapper.listener);

        // 탭에 연결2
        String tabId2 = "tabId2";
        EventSourceWrapper eventSourceWrapper2 = new EventSourceWrapper();

        final Request connectRequest2 = new Request.Builder()
                .url("http://localhost:" + port + "/connect?tabId=" + tabId2 + "&articleId=" + articleId)
                .build();
        factory.newEventSource(connectRequest2, eventSourceWrapper2.listener);

        // 연결이 완료될 때까지 대기
        sleep(2000);

        JSONObject json = new JSONObject();
        json.put("content", comment.getContent());
        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));

        Request postCommentRequest = new Request.Builder()
                .url("http://localhost:" + port + "/comment?tabId=" + tabId + "&articleId=" + articleId)
                .post(requestBody)
                .build();
        Call call = client.newCall(postCommentRequest);
        Response postResponse = call.execute();

        Assertions.assertEquals(HttpStatus.OK.value(), postResponse.code());

        // 댓글이 연결된 탭에 전송될 때까지 대기
        sleep(2000);

        // Assertions.assertEquals("connected!", eventSourceWrapper.receivedData.get(0));
        Assertions.assertEquals("connected!", eventSourceWrapper.receivedData.get(0));
        Assertions.assertEquals("{\"content\":\"Test content\",\"thumbsUp\":null}", eventSourceWrapper2.receivedData.get(1));
    }
    
    @NotThreadSafe
    private final class EventSourceWrapper {
    
        final EventSourceListener listener;
        final List<String> receivedData = new ArrayList<>();
    
        public EventSourceWrapper() {
            this.listener = new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {}
    
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    receivedData.add(data);
                }
    
                @Override
                public void onClosed(EventSource eventSource) {}
    
                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {}
            };
        }
    }
}
