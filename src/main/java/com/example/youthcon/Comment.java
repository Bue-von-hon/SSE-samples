package com.example.youthcon;

import java.util.concurrent.atomic.LongAdder;

public class Comment {

    private String content;

    private LongAdder thumbsUp;

    public Comment(final String content) {
        this.content = content;
        this.thumbsUp = new LongAdder();
    }

    public void thumbsUp() {
        thumbsUp.increment();
    }
}
