package org.likelionhsu.backend.naversearch.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

public class ExploreSummarizeDtos {

    @Data
    public static class UrlRequest {
        private String url;
    }

    @Data
    public static class UrlBatchRequest {
        private List<String> urls;
    }

    @Builder @Getter
    public static class ItemSummaryResponse {
        private String url;
        private String title;
        private String summary;      // ✅ 요약만 노출 (본문 금지)
        private String sourceType;   // "external" | "internal"
        private Instant publishedAt; // 있을 때만
    }

    @Builder @Getter
    public static class ItemSummaryListResponse {
        private List<ItemSummaryResponse> items;
    }
}
