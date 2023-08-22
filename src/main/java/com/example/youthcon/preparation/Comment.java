package com.example.youthcon.preparation;

import java.util.concurrent.atomic.LongAdder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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
