package org.likelionhsu.backend.user.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageCommentDto {
    private Long commentId;
    private Long postId;
    private String postTitle;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

