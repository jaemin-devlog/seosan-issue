package org.likelionhsu.backend.naversearch.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/naver-search")
public class NaverSearchController {

    private final NaverSearchService naverSearchService;
    private final PostService postService; // PostService 주입

    @GetMapping("/blogs-cafes")
    public ResponseEntity<List<Post>> searchNaverBlogsAndCafes(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int start) {
        List<Post> posts = naverSearchService.searchNaverBlogsAndCafes(query, display, start, "서산시 전체");
        if (!posts.isEmpty()) {
            postService.savePosts(posts); // 데이터베이스에 저장
        }
        return ResponseEntity.ok(posts);
    }
}
