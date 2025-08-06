package org.likelionhsu.backend.post.domain;

import java.util.stream.Stream;

public enum Category {
    NEWS,                // 뉴스
    WELFARE,             // 복지
    PUBLIC_INSTITUTION,  // 공공기관
    CULTURE,             // 문화소식
    CAFE,                // 카페
    BLOG,                // 블로그
    UNKNOWN;                // 알 수 없는 카테고리

    public static Category fromValue(String value) {
        return switch (value) {
            case "뉴스" -> NEWS;
            case "복지정보" -> WELFARE;
            case "공공기관", "시정소식", "고시공고", "타기관소식", "입법예고", "보도자료", "공지사항" -> PUBLIC_INSTITUTION;
            case "문화소식" -> CULTURE;
            case "카페" -> CAFE;
            case "블로그" -> BLOG;
            default -> throw new IllegalArgumentException("Unknown category: " + value);
        };
    }
}
