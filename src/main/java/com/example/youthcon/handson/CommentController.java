package com.example.youthcon.handson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.youthcon.preparation.Comment;
import com.example.youthcon.preparation.Connection;

@RestController
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;

    public CommentController(final CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(
                                            @RequestParam("tabId") final String tabId,
                                            @RequestParam("articleId") final String articleId) {
        final Connection connection = commentService.startViewingArticle(tabId, articleId);
        connection.connect();
        log.info("connected tabId : {}", tabId);
        return ResponseEntity.ok(connection.getEmitter());
    }

    @PostMapping("/comment")
    public ResponseEntity<Void> saveComment(
                    @RequestBody final Comment comment,
                    @RequestParam("tabId") final String tabId,
                    @RequestParam("articleId") final String articleId) {
        commentService.saveAndSend(comment, articleId, tabId);
        return ResponseEntity.ok().build();
    }

}
