package org.likelionhsu.backend.ai;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.ai.service.SummarizationOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai-search")
public class AiSearchController {

    private final SummarizationOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<?> search(@RequestBody Req req) {
        int n = (req.maxExternal == null || req.maxExternal <= 0) ? 3 : req.maxExternal;
        return ResponseEntity.ok(orchestrator.summarize(req.query, n));
    }

    @Data static class Req { String query; Integer maxExternal; }
}