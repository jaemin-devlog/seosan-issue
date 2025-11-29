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
public class MyPagePostDto {
    private Long postId;
    private String title;
    private String region;
    private String category;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime interactionAt;
}

