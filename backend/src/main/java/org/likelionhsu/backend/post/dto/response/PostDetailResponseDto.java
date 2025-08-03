package org.likelionhsu.backend.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Emotion;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.domain.WelfareCategory;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostDetailResponseDto {
    private final Long id; // 게시물 고유 식별자
    private final String title; // 게시물 제목
    private final String content; // 게시물 내용
    private final String link; // 원본 게시물 링크
    private final String pubDate; // 게시일
    private final String region; // 지역 (읍면동)
    private final Category category; // 게시물 대분류 카테고리
    private final WelfareCategory welfareCategory; // 복지 세부 카테고리
    private final Emotion emotion; // 감정 분석 결과
    private final String department; // 담당 부서
    private final Integer views; // 조회수
    private final LocalDateTime crawledAt; // 크롤링된 시간

    public static PostDetailResponseDto from(Post post) {
        return PostDetailResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .link(post.getLink())
                .pubDate(post.getPubDate())
                .region(post.getRegion())
                .category(post.getCategory())
                .welfareCategory(post.getWelfareCategory())
                .emotion(post.getEmotion())
                .department(post.getDepartment())
                .views(post.getViews())
                .crawledAt(post.getCrawledAt())
                .build();
    }
}
