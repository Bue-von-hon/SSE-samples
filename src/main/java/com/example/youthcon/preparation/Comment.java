package com.example.youthcon.preparation;

import java.util.concurrent.atomic.LongAdder;

public class Comment {

    private String content;

    private LongAdder thumbsUp;

    public Comment() {
    }

    public Comment(final String content) {
        this.content = content;
        this.thumbsUp = new LongAdder();
    }

    public Comment(String content, LongAdder thumbsUp) {
        this.content = content;
        this.thumbsUp = thumbsUp;
    }

    public void thumbsUp() {
        thumbsUp.increment();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LongAdder getThumbsUp() {
        return thumbsUp;
    }

    public void setThumbsUp(LongAdder thumbsUp) {
        this.thumbsUp = thumbsUp;
    }
}
