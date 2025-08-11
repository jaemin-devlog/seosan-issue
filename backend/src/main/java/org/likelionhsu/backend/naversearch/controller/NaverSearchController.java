package org.likelionhsu.backend.naversearch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/naver-search")
public class NaverSearchController {

    private final NaverSearchService naverSearchService;

    @PostMapping("/daily-trend")
    public ResponseEntity<JsonNode> getDailyTrends(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestBody List<Map<String, Object>> keywordGroups) {
        JsonNode trends = naverSearchService.getDailyTrends(startDate, endDate, keywordGroups);
        return ResponseEntity.ok(trends);
    }

    @PostMapping("/weekly-trend")
    public ResponseEntity<JsonNode> getWeeklyTrends(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestBody List<Map<String, Object>> keywordGroups) {
        JsonNode trends = naverSearchService.getWeeklyTrends(startDate, endDate, keywordGroups);
        return ResponseEntity.ok(trends);
    }
}