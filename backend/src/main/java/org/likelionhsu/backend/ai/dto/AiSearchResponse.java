package org.likelionhsu.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchResponse {
    private String summary;
    private List<String> sources; // 내부 공지 + 외부 링크
}
