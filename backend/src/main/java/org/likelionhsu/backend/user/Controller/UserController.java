package org.likelionhsu.backend.user.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.user.Dto.*;
import org.likelionhsu.backend.user.Service.MyPageService;
import org.likelionhsu.backend.user.Service.UserDetailsImpl;
import org.likelionhsu.backend.user.Service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MyPageService myPageService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        UserResponse response = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = userService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.deleteAccount(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserResponse response = userService.getUserInfo(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<Page<MyPagePostDto>> getMyBookmarks(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyPagePostDto> bookmarks = myPageService.getBookmarkedPosts(userDetails.getUser(), pageable);
        return ResponseEntity.ok(bookmarks);
    }

    @GetMapping("/me/likes")
    public ResponseEntity<Page<MyPagePostDto>> getMyLikes(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyPagePostDto> likes = myPageService.getLikedPosts(userDetails.getUser(), pageable);
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/me/comments")
    public ResponseEntity<Page<MyPageCommentDto>> getMyComments(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyPageCommentDto> comments = myPageService.getMyComments(userDetails.getUser(), pageable);
        return ResponseEntity.ok(comments);
    }
}

