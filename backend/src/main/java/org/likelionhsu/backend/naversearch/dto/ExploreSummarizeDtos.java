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

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemSummaryResponse {
        private String url;
        private String title;
        private String summary;
        private String sourceType;
        private Instant publishedAt;
    }

    @Builder @Getter
    public static class ItemSummaryListResponse {
        private List<ItemSummaryResponse> items;
    }
}
