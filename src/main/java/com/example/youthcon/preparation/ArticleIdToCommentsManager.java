package com.example.youthcon.preparation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ArticleIdToCommentsManager {
    // 아티클별 댓글
    private static final Map<String, List<Comment>> articleIdToComments = new ConcurrentHashMap<>();

     public void updateComments(final Comment comment, final String articleId) {
        final List<Comment> comments = articleIdToComments.computeIfAbsent(articleId, k -> new ArrayList<>());
        comments.add(comment);
        articleIdToComments.put(articleId, comments);
    }
}
