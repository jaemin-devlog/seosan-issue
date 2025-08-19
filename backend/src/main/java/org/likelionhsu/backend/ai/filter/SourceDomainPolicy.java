package org.likelionhsu.backend.ai.filter;

import java.net.URI;
import java.util.Set;

public final class SourceDomainPolicy {

    // 정말 막아야 할 도메인만 (예시)
    private static final Set<String> HARD_BLOCKED = Set.of(
            "example-spam.com"
    );

    // UGC: 검색 결과(/api/v1/ai-search)에서는 막고,
    // 탐색 요약(/api/v1/explore/summary)에서는 허용하고 싶어서 분리
    private static final Set<String> SOFT_BLOCKED = Set.of(
            "blog.naver.com", "m.blog.naver.com",
            "cafe.naver.com", "m.cafe.naver.com",
            "post.naver.com", "tistory.com", "brunch.co.kr"
    );

    private static final Set<String> TRUST_HINT = Set.of(
            "kma.go.kr", "korea.kr",
            "yonhapnews.co.kr", "newsis.com", "news1.kr",
            "hani.co.kr", "chosun.com", "donga.com", "joongang.co.kr",
            "kbs.co.kr", "mbc.co.kr", "sbs.co.kr"
    );

    public static boolean isHardBlocked(String url) {
        String h = host(url);
        return h == null || HARD_BLOCKED.stream().anyMatch(h::endsWith);
    }

    /** 전체 AI 검색에서는 소프트도 막는다 */
    public static boolean isBlocked(String url) {
        String h = host(url);
        if (h == null) return true;
        return HARD_BLOCKED.stream().anyMatch(h::endsWith)
                || SOFT_BLOCKED.stream().anyMatch(h::endsWith);
    }

    public static boolean isLikelyTrusted(String url) {
        String h = host(url);
        return h != null && TRUST_HINT.stream().anyMatch(h::endsWith);
    }

    public static String host(String url) {
        try { return URI.create(url).getHost(); } catch (Exception e) { return null; }
    }
}
