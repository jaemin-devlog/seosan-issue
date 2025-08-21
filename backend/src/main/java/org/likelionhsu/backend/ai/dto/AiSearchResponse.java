// backend/src/main/java/org/likelionhsu/backend/ai/dto/AiSearchResponse.java
package org.likelionhsu.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null은 응답에서 숨김
public class AiSearchResponse {

    private String summary;
    private List<String> sources; // 내부 공지 + 외부 링크

    // ▼ 추가(선택 필드) — 기존 호출부 깨지지 않도록 2-인자 생성자 유지
    private Boolean abstained;
    private String abstainReason;
    private List<String> noiseFlags;
    private List<String> droppedSources;

    // 기존 호출부 호환용 (두 인자 생성자 유지)
    public AiSearchResponse(String summary, List<String> sources) {
        this.summary = summary;
        this.sources = sources;
    }
}
