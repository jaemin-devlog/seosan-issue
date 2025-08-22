package org.likelionhsu.backend.ai;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.ai.dto.AiSearchDetailedResponse;
import org.likelionhsu.backend.ai.dto.AiSearchResponse;
import org.likelionhsu.backend.ai.service.SummarizationOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai-search")
public class AiSearchController {

    private final SummarizationOrchestrator orchestrator;

    /** 기존: 간단 요약(one-shot) */
    @PostMapping
    public ResponseEntity<AiSearchResponse> search(@RequestBody Req req) {
        int n = (req.maxExternal == null || req.maxExternal <= 0) ? 3 : req.maxExternal;
        return ResponseEntity.ok(orchestrator.summarize(req.query, n));
    }

    /** 신규: 문서별 요약 → 최종 재요약(Map-Reduce) */
    @PostMapping("/detail")
    public ResponseEntity<AiSearchDetailedResponse> searchDetailed(@RequestBody Req req) {
        int n = (req.maxExternal == null || req.maxExternal <= 0) ? 3 : req.maxExternal;
        return ResponseEntity.ok(orchestrator.summarizeDetailed(req.query, n));
    }

    /** 프롬프트 프리뷰(LLM 호출 없이 입력만 반환) */
    @GetMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam String query,
                                     @RequestParam(defaultValue = "3") int maxExternal) {
        return ResponseEntity.ok(orchestrator.buildPromptPreview(query, maxExternal));
    }

    @Data
    public static class Req {
        private String query;
        private Integer maxExternal;
    }
}
