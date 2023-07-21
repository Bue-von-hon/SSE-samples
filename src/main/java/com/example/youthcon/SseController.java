package com.example.youthcon;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Slf4j
public class SseController {

    private final SseEmitters sseEmitters;

    public SseController(SseEmitters sseEmitters) {
        this.sseEmitters = sseEmitters;
    }

    // todo(hun): 한명의 유저가 하나의 브라우저에 여러 탭을 실행중인 경우만 가정, 아티클도 하나
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(@RequestParam("tabId") final String tabId,
                                              @RequestParam("articleId") final String articleId) {
        log.info("tabId : {}", tabId);
        final SseEmitter emitter = sseEmitters.connect(tabId, articleId);
        try {
            final SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                                                                      .name("connect")
                                                                      .data("connected!")
                                                                      .reconnectTime(10000L);
            emitter.send(eventBuilder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(emitter);
    }

    @PostMapping("/count")
    public ResponseEntity<Void> count(@RequestParam("articleId") final String articleId) {
        sseEmitters.count(articleId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/tarticle")
    public void changeTaticle(@RequestParam("tabid") final String tabId,
                              @RequestParam("newarticleid") final String newArticleId,
                              @RequestParam("oldarticleid") final String oldArticleId) {
        sseEmitters.changeTaticle(tabId, newArticleId, oldArticleId);
    }

    @PostMapping("/disconnect")
    public void disconnect(@RequestParam("tabId") final String tabId,
                           @RequestParam("articleId") final String articleId) {
        log.info("cleanup called! tab's id : {}", tabId);
        sseEmitters.disconnect(tabId, articleId);
    }
}