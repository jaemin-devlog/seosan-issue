package org.likelionhsu.backend.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;

@Getter
@Builder
public class PostResponseDto {
    private final Long id;
    private final String title; // 게시물 제목
    private final String pubDate; // 게시일
    private final String region; // 지역 (읍면동)
    private final Category category; // 게시물 대분류 카테고리

    public static PostResponseDto from(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .pubDate(post.getPubDate())
                .region(post.getRegion())
                .category(post.getCategory())
                .build();
    }
}
