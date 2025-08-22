// backend/src/main/java/org/likelionhsu/backend/ai/dto/PerDocSummary.java
package org.likelionhsu.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerDocSummary(
        String url,
        String title,
        String sourceType,
        String publishedAt,
        String summary,
        String content
) {}
