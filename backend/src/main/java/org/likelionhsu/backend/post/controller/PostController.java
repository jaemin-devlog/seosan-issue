package org.likelionhsu.backend.post.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.dto.response.PostDetailResponseDto;
import org.likelionhsu.backend.post.dto.response.PostResponseDto;
import org.likelionhsu.backend.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getFilteredPosts(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Category categoryEnum = null;
        if (category != null && !category.isEmpty()) {
            categoryEnum = Category.fromValue(category);
        }
        Page<PostResponseDto> posts = postService.findPostsByFilter(region, categoryEnum, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDto> getPostById(@PathVariable Long postId) {
        PostDetailResponseDto post = postService.findPostById(postId);
        return ResponseEntity.ok(post);
    }
}

