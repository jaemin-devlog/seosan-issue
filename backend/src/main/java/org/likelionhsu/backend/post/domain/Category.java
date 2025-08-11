package org.likelionhsu.backend.post.domain;

import java.util.stream.Stream;

public enum Category {
    NEWS,                // 뉴스

    // 복지
    WELFARE_SENIOR,      // 복지정보-어르신
    WELFARE_DISABLED,    // 복지정보-장애인
    WELFARE_WOMEN_FAMILY,// 복지정보-여성가족
    WELFARE_CHILD_YOUTH, // 복지정보-아동청소년
    WELFARE_YOUTH,       // 복지정보-청년

    // 서산시청
    HEALTH_WELLNESS,     // 보건/건강
    NOTICE,              // 공지사항
    PRESS_RELEASE,       // 보도자료

    // 문화관광
    CULTURE_NEWS,        // 문화소식
    CITY_TOUR,           // 시티투어
    TOUR_GUIDE,          // 관광/안내

    CAFE,                // 카페
    BLOG,                // 블로그
    UNKNOWN;             // 알 수 없는 카테고리

    public static Category fromValue(String value) {
        return switch (value) {
            case "뉴스" -> NEWS;
            case "복지정보-어르신" -> WELFARE_SENIOR;
            case "복지정보-장애인" -> WELFARE_DISABLED;
            case "복지정보-여성가족" -> WELFARE_WOMEN_FAMILY;
            case "복지정보-아동청소년" -> WELFARE_CHILD_YOUTH;
            case "복지정보-청년" -> WELFARE_YOUTH;
            case "보건-건강" -> HEALTH_WELLNESS;
            case "공지사항" -> NOTICE;
            case "보도자료" -> PRESS_RELEASE;
            case "문화소식" -> CULTURE_NEWS;
            case "시티투어" -> CITY_TOUR;
            case "관광-안내" -> TOUR_GUIDE;
            case "카페" -> CAFE;
            case "블로그" -> BLOG;
            default -> throw new IllegalArgumentException("Unknown or unhandled category: " + value);
        };
    }
}
