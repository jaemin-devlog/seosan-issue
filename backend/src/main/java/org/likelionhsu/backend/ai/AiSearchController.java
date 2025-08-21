package org.likelionhsu.backend.ai;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.AiSearchDetailedResponse;
import org.likelionhsu.backend.ai.dto.AiSearchResponse;
import org.likelionhsu.backend.ai.service.SummarizationOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai-search")
public class AiSearchController {

    private final SummarizationOrchestrator orchestrator;

    /** 간단 요약 (one-shot) */
    @PostMapping
    public ResponseEntity<AiSearchResponse> search(@RequestBody Req req) {
        if (req == null || StringUtils.isBlank(req.getQuery())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AiSearchResponse("", Collections.emptyList()));
        }
        int n = (req.getMaxExternal() == null || req.getMaxExternal() <= 0) ? 3 : req.getMaxExternal();

        // TODO: 여기서 query 기반 내부/외부 문서 수집 후 candidateDocs / sources 구성
        List<String> candidateDocs = Collections.emptyList();
        List<String> sources = Collections.emptyList();

        return orchestrator.oneshot(candidateDocs, sources)
                .map(ResponseEntity::ok)                       // 정상 → 200
                .defaultIfEmpty(ResponseEntity.noContent().build()) // Mono.empty() → 204
                .block();
    }

    /** 문서별 요약 → 최종 재요약 (Map-Reduce) */
    @PostMapping("/detail")
    public ResponseEntity<AiSearchDetailedResponse> searchDetailed(@RequestBody Req req) {
        if (req == null || StringUtils.isBlank(req.getQuery())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AiSearchDetailedResponse(null, Collections.emptyList(), Collections.emptyList()));
        }
        int n = (req.getMaxExternal() == null || req.getMaxExternal() <= 0) ? 3 : req.getMaxExternal();

        // TODO: 여기서 query 기반 내부/외부 문서 수집 후 docs / sources 구성
        List<SummarizationOrchestrator.Doc> docs = Collections.emptyList();
        List<String> sources = Collections.emptyList();

        return orchestrator.detailed(docs, sources)
                .map(ResponseEntity::ok)                       // 정상 → 200
                .defaultIfEmpty(ResponseEntity.noContent().build()) // Mono.empty() → 204
                .block();
    }

    /** 프롬프트 프리뷰(LLM 호출 없이 입력만 반환) */
    @GetMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam String query,
                                     @RequestParam(defaultValue = "3") int maxExternal) {
        if (StringUtils.isBlank(query)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("query is required");
        }
        // 오케스트레이터가 system/user 조합만 반환 (LLM 호출 없음)
        return ResponseEntity.ok(orchestrator.buildPromptPreview(query, maxExternal));
    }

    @Data
    public static class Req {
        private String query;
        private Integer maxExternal;
    }
}
