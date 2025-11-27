package org.likelionhsu.backend.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 게시물 고유 식별자

    @Column(nullable = false)
    private String title; // 게시물 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 게시물 내용

    @Column(nullable = false)
    private String link; // 원본 게시물 링크

    @Column(nullable = false)
    private String pubDate; // 게시일 (크롤링한 데이터의 date)

    @Column(nullable = false)
    private String region; // 지역 (읍면동 또는 '서산시 전체')

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category; // 게시물 대분류 카테고리

    private String department; // 담당 부서

    private Integer views; // 조회수

    @Column(nullable = false)
    private LocalDateTime crawledAt; // 크롤링된 시간

    // --- 추가: 게시물 소스 구분 및 외부 식별자 ---

    /**
     * 게시물이 어떤 소스(서산시 공지, 네이버 뉴스 등)에서 온 것인지 구분하기 위한 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    /**
     * 외부 시스템(NAVER 등)에서 사용하는 식별자 또는 URL.
     * 예: 네이버 뉴스의 경우 기사 URL을 저장해 중복 저장을 막는 데 사용.
     */
    @Column(name = "external_id")
    private String externalId;

    @Builder
    public Post(String title,
                String content,
                String link,
                String pubDate,
                String region,
                Category category,
                String department,
                Integer views,
                LocalDateTime crawledAt,
                SourceType sourceType,
                String externalId) {
        this.title = title;
        this.content = content;
        this.link = link;
        this.pubDate = pubDate;
        this.region = region;
        this.category = category;
        this.department = department;
        this.views = views;
        this.crawledAt = crawledAt;
        this.sourceType = sourceType;
        this.externalId = externalId;
    }
}
