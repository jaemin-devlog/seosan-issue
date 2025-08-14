package org.likelionhsu.backend.ai.dto;

public record PerDocSummary(
        String url,
        String title,
        String sourceType,   // "internal" | "external" (또는 news/blog/cafe 등)
        String publishedAt,  // 추출 가능 시 채우기, 아니면 null
        String summary       // 1~2문장 요약
) {}
