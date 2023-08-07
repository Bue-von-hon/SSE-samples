package com.example.youthcon.adapter.inbound;

import com.example.youthcon.Comment;

public class PostCommentRequest {

    private String content;

    private int thumbsUp;

    public String getContent() {
        return content;
    }

    public int getThumbsUp() {
        return thumbsUp;
    }

    public PostCommentRequest(final String content, final int thumbsUp) {
        this.content = content;
        this.thumbsUp = thumbsUp;
    }

    public static Comment toComment(final PostCommentRequest request) {
        return new Comment(request.getContent(), request.getThumbsUp());
    }
}
