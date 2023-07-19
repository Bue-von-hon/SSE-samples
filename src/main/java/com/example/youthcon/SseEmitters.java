package com.example.youthcon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Slf4j
public class SseEmitters {

    // todo(hun): 아티클별 조회수
    private static final ConcurrentHashMap<String, AtomicLong> articleId2AtomicLong = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, SseEmitter> tabId2SseEmitter = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<String>> articleId2tabId = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> tabId2Article = new ConcurrentHashMap<>();

    SseEmitter connect(final String tabId, final String articleId) {
        // 기존에 다른 아티클을 보고있다가, 동일 탭에서 다른 아티클을 클릭한 경우
        final String previousArticleId = tabId2Article.get(tabId);
        if (previousArticleId != null && !previousArticleId.equals(articleId)) {
            replaceOldArticle2NewArticle(tabId, articleId, previousArticleId);
        }
        else {
            final SseEmitter emitter = new SseEmitter();
            emitter.onCompletion(() -> {
                /*
                    1. 탭-커넥션 해쉬맵에서 현재 탭과 연결된 커넥션 제거
                    2. 아티클-탭 해쉬맵에서 현재 아티클과 연결된 탭 제거
                    3. 탭-아티클 해쉬맵에서 현재 탭과 연결된 아티클 제거
                */
                cleanup(tabId, articleId);
                log.info("onCompletion callback tab's id : {}", tabId);
            });
            emitter.onTimeout(() -> {
                log.info("onTimeout callback tab's id : {}", tabId);
                emitter.complete();
            });

            /*
                처음 아티클을 열었을때, 탭 아이디를 추가하는 케이스
                1. 새로운 커넥션 생성
                2. 아티클-탭 해쉬맵에 새로운 아티클과 현재 탭을 매핑
                3. 탭-커넥션 해쉬맵에 현재 탭과 새로운 커넥션을 매핑
                4. 탭-아티클 해쉬맵에 현재 탭과 새로운 아티클을 매핑
             */
            articleId2tabId.computeIfAbsent(articleId, k -> new ArrayList<>()).add(tabId);
            tabId2SseEmitter.putIfAbsent(tabId, emitter);
            tabId2Article.putIfAbsent(tabId, articleId);
        }

        final SseEmitter emitter = tabId2SseEmitter.get(tabId);
        if (emitter == null) {
            throw new RuntimeException("emitter can not be null");
        }
        log.info("emitter list size: {}", tabId2SseEmitter.size());
        log.info("emitter : {}", emitter);
        return emitter;
    }

    public void count(final String articleId) {
        final AtomicLong counter = articleId2AtomicLong.computeIfAbsent(articleId, k -> new AtomicLong());
        final long count = counter.incrementAndGet();
        articleId2AtomicLong.put(articleId, counter);

        final List<String> tabIdList = articleId2tabId.getOrDefault(articleId, new ArrayList<>());
        tabIdList.forEach(tabId -> {
            final SseEmitter emitter = tabId2SseEmitter.get(tabId);
            if (emitter == null) {
                throw new RuntimeException("emitter can not be null");
            }
            try {
                emitter.send(SseEmitter.event()
                                       .name("count")
                                       .data(count));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // 아티클을 보고 있던 탭에서 다른 아티클로 넘어갔다는 요청이 전송된 경우
    public void changeTaticle(final String tabId, final String articleId) {
        final String previousArticleId = tabId2Article.get(tabId);
        if (previousArticleId != null) {
            replaceOldArticle2NewArticle(tabId, articleId, previousArticleId);
        }
        log.info("tabId : {}", tabId);
        log.info("previousArticleId : {}", previousArticleId);
        log.info("articleId : {}", articleId);
    }

    /*
        1. 탭-아티클 해쉬맵 값을 업데이트
        2. 아티클-탭 해쉬맵에서 이전의 아티클에 매핑되어 있던 리스트에서 현제 탭을 제거하고 다시 put
        3. 아티클-탭 해쉬맵에 새로운 아티클과 탭을 매핑
    */
    private void replaceOldArticle2NewArticle(String tabId, String newArticleId, String oldArticleId) {
        tabId2Article.put(tabId, newArticleId);
        final List<String> tabIdList = articleId2tabId.getOrDefault(oldArticleId, new ArrayList<>());
        tabIdList.remove(oldArticleId);
        articleId2tabId.put(oldArticleId, tabIdList);
        articleId2tabId.computeIfAbsent(newArticleId, k -> new ArrayList<>()).add(tabId);
    }

    public void disconnect(final String tabId, final String articleId) {
        cleanup(tabId, articleId);
    }

    private void cleanup(String tabId, String articleId) {
        tabId2SseEmitter.remove(tabId);
        final List<String> tabIdList = articleId2tabId.getOrDefault(articleId, new ArrayList<>());
        tabIdList.remove(articleId);
        articleId2tabId.put(articleId, tabIdList);
        tabId2Article.remove(tabId);
    }
}