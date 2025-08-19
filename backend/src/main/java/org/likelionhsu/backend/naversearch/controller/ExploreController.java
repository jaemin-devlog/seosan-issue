package org.likelionhsu.backend.naversearch.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.naversearch.NaverSearchItemDto;
import org.likelionhsu.backend.naversearch.dto.ExploreSummarizeDtos;
import org.likelionhsu.backend.naversearch.service.ExploreSummarizeService;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/explore")
public class ExploreController {

    private final NaverSearchService naverSearchService;
    private final ExploreSummarizeService exploreSummarizeService;

    @GetMapping("/naver")
    public ResponseEntity<List<NaverSearchItemDto>> explore(@RequestParam("q") String query,
                                                            @RequestParam(value="types", required=false) List<String> types,
                                                            @RequestParam(value="display", defaultValue="10") int display) {
        if (types == null || types.isEmpty()) types = Arrays.asList("news","blog","cafearticle");
        List<NaverSearchItemDto> all = new ArrayList<>();
        for (String t : types) all.addAll(naverSearchService.search(t, query, display));
        return ResponseEntity.ok(all); // 프론트에서 title/description/link 바로 노출
    }

    // ✅ 단건 URL 요약 (탐색 리스트에서 항목 클릭 시 호출)
    @PostMapping("/summary")
    public ResponseEntity<ExploreSummarizeDtos.ItemSummaryResponse> summarizeByUrl(@RequestBody ExploreSummarizeDtos.UrlRequest req) {
        if (req == null || req.getUrl() == null || req.getUrl().isBlank())
            return ResponseEntity.badRequest().build();


        var res = exploreSummarizeService.summarizeUrl(req.getUrl());
        if (res == null) return ResponseEntity.noContent().build(); // 품질 미달/차단/실패
        return ResponseEntity.ok(res);
    }

    // ✅ 배치 URL 요약 (선택한 여러 항목을 한꺼번에)
    @PostMapping("/summary/batch")
    public ResponseEntity<ExploreSummarizeDtos.ItemSummaryListResponse> summarizeBatch(@RequestBody ExploreSummarizeDtos.UrlBatchRequest req) {
        if (req == null || req.getUrls() == null || req.getUrls().isEmpty())
            return ResponseEntity.badRequest().build();

        List<ExploreSummarizeDtos.ItemSummaryResponse> out = req.getUrls().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(exploreSummarizeService::summarizeUrl)
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(ExploreSummarizeDtos.ItemSummaryListResponse.builder().items(out).build());
    }
}