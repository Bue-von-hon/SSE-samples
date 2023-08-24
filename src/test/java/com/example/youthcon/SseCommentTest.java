package com.example.youthcon;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import com.example.youthcon.preparation.Comment;
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
class SseCommentTest {

    @LocalServerPort
    private int port;

    private static final OkHttpClient client = new OkHttpClient();

    private static final EventSource.Factory factory = EventSources.createFactory(client);

    private static final JSONObject json = new JSONObject();

    @Test
    void shouldConnectToSseEmitterAndReceiveConnectedEvent() throws InterruptedException {
        final String articleId = "articleId";
        final String tabId = "tabId";
        final Request request = getConnectRequest(tabId, articleId);
        final EventSourceWrapper eventWrapper = new EventSourceWrapper();
        factory.newEventSource(request, eventWrapper.listener);

        // wait for the connection and the first event
        sleep(2000);

        Assertions.assertEquals("connected!", eventWrapper.receivedData.get(0));
    }

    @Test
    void shouldSaveAndSendCommentToConnectedTabs() throws InterruptedException, JSONException, IOException {
        // 탭에 연결1
        final String articleId1 = "articleId1";
        final String tabId1 = "tabId1";
        final Request connectRequest = getConnectRequest(tabId1, articleId1);
        final EventSourceWrapper eventSourceWrapper = new EventSourceWrapper();
        factory.newEventSource(connectRequest, eventSourceWrapper.listener);

        // 탭에 연결2
        final String tabId2 = "tabId2";
        final EventSourceWrapper eventSourceWrapper2 = new EventSourceWrapper();
        final Request connectRequest2 = getConnectRequest(tabId2, articleId1);
        factory.newEventSource(connectRequest2, eventSourceWrapper2.listener);

        // 연결이 완료될 때까지 대기
        sleep(2000);

        final Comment comment = new Comment("Test content");
        final Response postResponse = getPostResponse(articleId1, tabId1, comment);

        Assertions.assertEquals(HttpStatus.OK.value(), postResponse.code());

        // 댓글이 연결된 탭에 전송될 때까지 대기
        sleep(2000);

        Assertions.assertEquals("connected!", eventSourceWrapper.receivedData.get(0));
        Assertions.assertEquals("{\"content\":\"Test content\"}",
                eventSourceWrapper2.receivedData.get(1));
    }

    @Test
    void shouldDisconnectAfterTimeout() throws InterruptedException {
        final String articleId = "articleId";
        final String tabId = "tabId";
        final Request request = getConnectRequest(tabId, articleId);
        final EventSourceWrapper eventWrapper = new EventSourceWrapper();
        factory.newEventSource(request, eventWrapper.listener);

        // 연결이 완료될 때까지 대기
        sleep(2000);

        Assertions.assertEquals("connected!", eventWrapper.receivedData.get(0));

        // 연결이 타임아웃될 때까지 대기
        sleep(10000);

        Assertions.assertTrue(eventWrapper.onFailureCalled);
    }

    private Response getPostResponse(String articleId1, String tabId1, Comment comment)
            throws JSONException, IOException {
        json.put("content", comment.getContent());
        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request postCommentRequest = getPostCommentRequest(articleId1, tabId1, requestBody);
        Call call = client.newCall(postCommentRequest);
        Response postResponse = call.execute();
        return postResponse;
    }

    private Request getPostCommentRequest(String articleId1, String tabId1, RequestBody requestBody) {
        return new Request.Builder()
                .url("http://localhost:" + port + "/comment?tabId=" + tabId1 + "&articleId=" + articleId1)
                .post(requestBody)
                .build();
    }

    private Request getConnectRequest(final String tabId, final String articleId) {
        return new Request.Builder()
                .url("http://localhost:" + port + "/connect?tabId=" + tabId + "&articleId=" + articleId).build();
    }

    @NotThreadSafe
    private final class EventSourceWrapper {

        final EventSourceListener listener;
        final List<String> receivedData = new ArrayList<>();
        boolean isOpened = false;
        boolean isClosed = false;
        boolean onFailureCalled = false;

        public EventSourceWrapper() {
            this.listener = new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    isOpened = true;
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    receivedData.add(data);
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    isClosed = true;
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    onFailureCalled = true;
                }
            };
        }
    }
}
