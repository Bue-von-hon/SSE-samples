package com.example.youthcon;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.example.youthcon.preparation.Comment;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"sse.timeout=1000", "sse.reconnect-time=500"})
class SseCommentTest {

    @LocalServerPort
    private int port;

    private static final OkHttpClient client = new OkHttpClient();

    private static final EventSource.Factory factory = EventSources.createFactory(client);

    private static final JSONObject json = new JSONObject();

    @Test
    @DisplayName("SSE 연결에 성공한다")
    void shouldConnectToSseEmitterAndReceiveConnectedEvent() {
        TestSseConnection connection = connect("tabId", "articleId", 1);

        assertThat(connection.isOpened()).isTrue();
        assertThat(connection.getReceivedData()).containsExactly("connected!");
    }

    @Test
    @DisplayName("SSE 연결 후 데이터를 받는다")
    void shouldSaveAndSendCommentToConnectedTabs() throws JSONException, IOException {
        TestSseConnection connection1 = connect("tabId1", "articleId", 2);
        TestSseConnection connection2 = connect("tabId2", "articleId", 2);

        Response response = saveComment("articleId", "tabId1", new Comment("Test content"));

        assertThat(response.code()).isEqualTo(HttpStatus.OK.value());
        assertThat(connection1.getReceivedData()).containsExactly("connected!", "{\"content\":\"Test content\"}");
        assertThat(connection2.getReceivedData()).containsExactly("connected!", "{\"content\":\"Test content\"}");
    }

    @Test
    @DisplayName("제한시간이 지난 후 SSE 연결이 종료된다")
    void shouldDisconnectAfterTimeout() throws InterruptedException {
        TestSseConnection connection = connect("tabId", "articleId", 1);

        assertThat(connection.isOpened()).isTrue();
        sleep(1000); // waiting for close
        assertThat(connection.isClosed()).isTrue();
    }

    private TestSseConnection connect(String tabId, String articleId, int receivedEventSize) {
        Request connectRequest = new Request.Builder()
                .url("http://localhost:" + port + "/connect?tabId=" + tabId + "&articleId=" + articleId)
                .build();
        TestSseConnection connection = new TestSseConnection(receivedEventSize);
        factory.newEventSource(connectRequest, connection.listener());
        return connection;
    }

    private Response saveComment(String articleId, String tabId, Comment comment)
            throws JSONException, IOException {
        json.put("content", comment.getContent());
        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request postCommentRequest = new Request.Builder()
                .url("http://localhost:" + port + "/comment?tabId=" + tabId + "&articleId=" + articleId)
                .post(requestBody)
                .build();
        return client.newCall(postCommentRequest).execute();
    }
}
