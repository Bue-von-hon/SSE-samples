package com.example.youthcon;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(@RequestParam("tabId") final String tabId,
                                              @RequestParam("articleId") final String articleId) {
        log.info("tabId : {}", tabId);
        final Connection connection = sseEmitters.connect(tabId, articleId);
        connection.connect();
        return ResponseEntity.ok(connection.getEmitter());
    }

    @PostMapping("/count")
    public ResponseEntity<Void> count(@RequestParam("articleId") final String articleId) {
        sseEmitters.count(articleId);
        return ResponseEntity.ok().build();
    }
}
