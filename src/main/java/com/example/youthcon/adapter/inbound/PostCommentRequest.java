package com.example.youthcon.adapter.inbound;

import com.example.youthcon.Comment;

public class PostCommentRequest {

    private String content;

    public String getContent() {
        return content;
    }

    public PostCommentRequest(final String content) {
        this.content = content;
    }

    public static Comment toComment(final PostCommentRequest request) {
        return new Comment(request.getContent());
    }
}
