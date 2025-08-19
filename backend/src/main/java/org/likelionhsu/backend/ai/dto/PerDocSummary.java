package org.likelionhsu.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 응답에서 생략
public record PerDocSummary(
        String url,
        String title,
        String sourceType,   // "internal" | "external" | "news" | "blog" ...
        String publishedAt,  // 추출 가능 시
        String summary,      // 1~2문장 요약 (없을 수 있음)
        String content       // ✅ 요약이 없을 때라도 원문 텍스트를 내려주기 위한 필드 (nullable)
) {}
