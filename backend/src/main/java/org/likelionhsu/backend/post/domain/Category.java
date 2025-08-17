package org.likelionhsu.backend.post.domain;

import java.util.*;

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

    // ---- 새로 추가: 별칭/표기 변형 흡수용 맵 ----
    private static final Map<String, Category> ALIASES = new HashMap<>();

    static {
        // 공통 규칙: 전처리 후 키를 넣음(아래 normalize 참고)
        // 한글 표기
        putAlias("뉴스", NEWS);

        putAlias("복지정보어르신", WELFARE_SENIOR);
        putAlias("복지정보-어르신", WELFARE_SENIOR);

        putAlias("복지정보장애인", WELFARE_DISABLED);
        putAlias("복지정보-장애인", WELFARE_DISABLED);

        putAlias("복지정보여성가족", WELFARE_WOMEN_FAMILY);
        putAlias("복지정보-여성가족", WELFARE_WOMEN_FAMILY);

        putAlias("복지정보아동청소년", WELFARE_CHILD_YOUTH);
        putAlias("복지정보-아동청소년", WELFARE_CHILD_YOUTH);

        putAlias("복지정보청년", WELFARE_YOUTH);
        putAlias("복지정보-청년", WELFARE_YOUTH);

        putAlias("보건건강", HEALTH_WELLNESS);
        putAlias("보건/건강", HEALTH_WELLNESS);
        putAlias("보건-건강", HEALTH_WELLNESS);

        putAlias("공지", NOTICE);
        putAlias("공지사항", NOTICE);

        putAlias("보도자료", PRESS_RELEASE);

        putAlias("문화소식", CULTURE_NEWS);
        putAlias("시티투어", CITY_TOUR);
        putAlias("관광안내", TOUR_GUIDE);
        putAlias("관광/안내", TOUR_GUIDE);
        putAlias("관광-안내", TOUR_GUIDE);

        putAlias("카페", CAFE);
        putAlias("블로그", BLOG);

        // 영문 enum 문자열 그대로 들어오는 경우도 흡수
        for (Category c : values()) {
            putAlias(c.name(), c); // NOTICE, PRESS_RELEASE 등
        }
    }

    private static void putAlias(String raw, Category c) {
        ALIASES.put(normalize(raw), c);
    }

    // 입력 전처리: 트림 → 소문자 → 한글/영문/숫자 외 구분자 제거(슬래시/하이픈/공백 등)
    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        // 구분자 제거: 공백, 슬래시, 하이픈, 밑줄, 점
        t = t.replaceAll("[s/_-.]+", "");
        return t;
    }

    /** 모르는 값이면 예외(컨트롤러에서 400 처리 용) */
    public static Category fromValue(String value) {
        Category c = ALIASES.get(normalize(value));
        if (c == null) {
            throw new IllegalArgumentException("Unknown or unhandled category: " + value);
        }
        return c;
    }

    /** 모르는 값이면 UNKNOWN으로 폴백(크롤러/저장 파이프라인 등에서 사용) */
    public static Category fromValueOrUnknown(String value) {
        Category c = ALIASES.get(normalize(value));
        return c != null ? c : UNKNOWN;
    }
}