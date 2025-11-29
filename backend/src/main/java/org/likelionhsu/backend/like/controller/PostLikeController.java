package org.likelionhsu.backend.like.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.like.service.PostLikeService;
import org.likelionhsu.backend.user.Service.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        postLikeService.likePost(postId, userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        postLikeService.unlikePost(postId, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
