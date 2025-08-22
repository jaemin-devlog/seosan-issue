package org.likelionhsu.backend.ai.dto;

public record ArticleText(
        String url,
        String title,
        String text,
        String extra
) {}
