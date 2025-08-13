package org.likelionhsu.backend.ai.dto;

import lombok.Data;

@Data
public class AiSearchRequest {
    private String query;
    private Integer maxExternal = 3; // 외부 본문 상위 N
}
