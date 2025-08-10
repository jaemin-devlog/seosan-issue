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

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Category category; // 게시물 대분류 카테고리

    private String department; // 담당 부서

    private Integer views; // 조회수

    @Column(nullable = false)
    private LocalDateTime crawledAt; // 크롤링된 시간


    @Builder
    public Post(String title, String content, String link, String pubDate, String region, Category category, String department, Integer views, LocalDateTime crawledAt) {
        this.title = title;
        this.content = content;
        this.link = link;
        this.pubDate = pubDate;
        this.region = region;
        this.category = category;
        this.department = department;
        this.views = views;
        this.crawledAt = crawledAt;
    }
}
