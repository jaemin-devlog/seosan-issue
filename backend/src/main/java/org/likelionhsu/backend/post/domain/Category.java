package org.likelionhsu.backend.post.domain;

import java.util.*;

public enum Category {
    NEWS,
    WELFARE_SENIOR,
    WELFARE_DISABLED,
    WELFARE_WOMEN_FAMILY,
    WELFARE_CHILD_YOUTH,
    WELFARE_YOUTH,
    HEALTH_WELLNESS,
    NOTICE,
    PRESS_RELEASE,
    CULTURE_NEWS,
    CITY_TOUR,
    TOUR_GUIDE,
    CAFE,
    BLOG,
    UNKNOWN;

    private static final Map<String, Category> ALIASES = new HashMap<>();

    static {
        // 한글 라벨
        putAlias("뉴스", NEWS);

        putAlias("복지정보-어르신", WELFARE_SENIOR);
        putAlias("복지정보어르신", WELFARE_SENIOR);

        putAlias("복지정보-장애인", WELFARE_DISABLED);
        putAlias("복지정보장애인", WELFARE_DISABLED);

        putAlias("복지정보-여성가족", WELFARE_WOMEN_FAMILY);
        putAlias("복지정보여성가족", WELFARE_WOMEN_FAMILY);

        putAlias("복지정보-아동청소년", WELFARE_CHILD_YOUTH);
        putAlias("복지정보아동청소년", WELFARE_CHILD_YOUTH);

        putAlias("복지정보-청년", WELFARE_YOUTH);
        putAlias("복지정보청년", WELFARE_YOUTH);

        putAlias("보건/건강", HEALTH_WELLNESS);
        putAlias("보건-건강", HEALTH_WELLNESS);
        putAlias("보건건강", HEALTH_WELLNESS);

        putAlias("공지사항", NOTICE);
        putAlias("공지", NOTICE);

        putAlias("보도자료", PRESS_RELEASE);

        putAlias("문화소식", CULTURE_NEWS);
        putAlias("시티투어", CITY_TOUR);

        putAlias("관광/안내", TOUR_GUIDE);
        putAlias("관광-안내", TOUR_GUIDE);
        putAlias("관광안내", TOUR_GUIDE);

        putAlias("카페", CAFE);
        putAlias("블로그", BLOG);

        // 영문 enum 명칭도 전부 허용
        for (Category c : values()) {
            putAlias(c.name(), c);
        }
    }

    private static void putAlias(String raw, Category c) {
        ALIASES.put(normalize(raw), c);
    }

    // ⚠️ 여기 수정: 하이픈은 클래스 맨 앞에 두거나 이스케이프, \s는 자바 문자열에서 \\s
    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        // 공백/슬래시/언더스코어/점/하이픈 제거
        // 자바 문자열 → 정규식으로 들어갈 때: \\s, \\. , \\- 가 맞음
        t = t.replaceAll("[-\\s/_\\.]+", "");  // 하이픈을 첫 문자로 둠(범위 해석 방지)
        return t;
    }

    public static Category fromValue(String value) {
        Category c = ALIASES.get(normalize(value));
        if (c == null) {
            throw new IllegalArgumentException("Unknown or unhandled category: " + value);
        }
        return c;
    }

    public static Category fromValueOrUnknown(String value) {
        Category c = ALIASES.get(normalize(value));
        return c != null ? c : UNKNOWN;
    }
}
