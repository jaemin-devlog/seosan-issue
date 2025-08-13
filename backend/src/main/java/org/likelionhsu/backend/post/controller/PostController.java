package org.likelionhsu.backend.post.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.dto.response.PostDetailResponseDto;
import org.likelionhsu.backend.post.dto.response.PostResponseDto;
import org.likelionhsu.backend.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getFilteredPosts(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponseDto> posts = postService.findPostsByFilter(region, category, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDto> getPostById(@PathVariable Long postId) {
        PostDetailResponseDto post = postService.findPostById(postId);
        return ResponseEntity.ok(post);
    }
}

