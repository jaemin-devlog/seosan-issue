package org.likelionhsu.backend.bookmark.controller;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.bookmark.service.BookmarkService;
import org.likelionhsu.backend.user.Service.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addBookmark(@PathVariable Long postId,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        bookmarkService.addBookmark(postId, userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long postId,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        bookmarkService.removeBookmark(postId, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}

