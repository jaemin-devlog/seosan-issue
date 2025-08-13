package org.likelionhsu.backend.naversearch.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.naversearch.NaverSearchItemDto;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/explore")
public class ExploreController {

    private final NaverSearchService naverSearchService;

    @GetMapping("/naver")
    public ResponseEntity<List<NaverSearchItemDto>> explore(@RequestParam("q") String query,
                                                            @RequestParam(value="types", required=false) List<String> types,
                                                            @RequestParam(value="display", defaultValue="10") int display) {
        if (types == null || types.isEmpty()) types = Arrays.asList("news","blog","cafearticle");
        List<NaverSearchItemDto> all = new ArrayList<>();
        for (String t : types) all.addAll(naverSearchService.search(t, query, display));
        return ResponseEntity.ok(all); // 프론트에서 title/description/link 바로 노출
    }
}