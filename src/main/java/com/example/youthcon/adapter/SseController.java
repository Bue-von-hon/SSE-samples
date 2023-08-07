package com.example.youthcon.adapter;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.youthcon.adapter.inbound.PostCommentRequest;
import com.example.youthcon.application.CommentService;
import com.example.youthcon.application.Connection;
import com.example.youthcon.application.SseEmitters;

@RestController
@Slf4j
public class SseController {

    private final SseEmitters sseEmitters;

    private final CommentService commentService;

    public SseController(final SseEmitters sseEmitters, final CommentService commentService) {
        this.sseEmitters = sseEmitters;
        this.commentService = commentService;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(
                                            @RequestParam("tabId") final String tabId,
                                            @RequestParam("articleId") final String articleId) {
        final Connection connection = sseEmitters.connect(tabId, articleId);
        connection.connect();
        log.info("connected tabId : {}", tabId);
        return ResponseEntity.ok(connection.getEmitter());
    }

    @PostMapping("/count")
    public ResponseEntity<Void> count(
                    @RequestParam("articleId") final String articleId) {
        sseEmitters.count(articleId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comment")
    public ResponseEntity<Void> saveComment(
                    @RequestBody final PostCommentRequest commentRequest,
                    @RequestParam("tabId") final String tabId,
                    @RequestParam("articleId") final String articleId) {
        commentService.saveAndSend(PostCommentRequest.toComment(commentRequest), articleId, tabId);
        return ResponseEntity.ok().build();
    }

}
