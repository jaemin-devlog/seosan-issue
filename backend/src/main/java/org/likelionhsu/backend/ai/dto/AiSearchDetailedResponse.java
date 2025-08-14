package org.likelionhsu.backend.ai.dto;

import java.util.List;

public record AiSearchDetailedResponse(
        String tldr,                 // 최종 재요약(3~5줄)
        List<PerDocSummary> items,   // 출처별 1~2문장 요약
        List<String> sources         // 링크 목록
) {}
