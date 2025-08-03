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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getFilteredPosts(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Category category) {
        List<PostResponseDto> posts = postService.findPostsByFilter(region, category);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDto> getPostById(@PathVariable Long postId) {
        PostDetailResponseDto post = postService.findPostById(postId);
        return ResponseEntity.ok(post);
    }
}

