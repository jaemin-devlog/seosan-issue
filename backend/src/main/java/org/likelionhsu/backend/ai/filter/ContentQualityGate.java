package org.likelionhsu.backend.ai.filter;

import java.util.Set;

public final class ContentQualityGate {

    private static final int MIN_LEN_DEFAULT = 380;
    private static final int MIN_LEN_UGC     = 260; // 블로그/카페는 더 관대
    private static final int NOISE_THRESHOLD = 50;  // 비정상 기호 밀집 컷

    private ContentQualityGate() {}

    public static boolean pass(String title, String body, Set<String> mustTerms) {
        return pass(title, body, mustTerms, null);
    }

    public static boolean pass(String title, String body, Set<String> mustTerms, String host) {
        if (body == null) return false;

        final boolean isUGC = host != null &&
                (host.contains("blog.naver.com") || host.contains("cafe.naver.com")
                        || host.contains("m.blog.naver.com") || host.contains("m.cafe.naver.com"));

        final int minLen = isUGC ? MIN_LEN_UGC : MIN_LEN_DEFAULT;
        if (body.strip().length() < minLen) return false;

        final String t = normalize(title);
        final String b = normalize(body);

        // 🔑 UGC는 키워드 매칭이 0개여도 통과(길이/노이즈로만 거름)
        if (!isUGC && mustTerms != null && !mustTerms.isEmpty()) {
            // 일반 매체: 최소 2개(또는 mustTerms.size() 이하)
            final int requiredHits = Math.min(2, mustTerms.size());
            long hits = mustTerms.stream().filter(term -> t.contains(term) || b.contains(term)).count();
            if (hits < requiredHits) return false;
        }
        // 비정상 기호 밀집 컷
        if (b.replaceAll("[가-힣a-zA-Z0-9\\s.,:;()\\-]", "").length() > NOISE_THRESHOLD) return false;

        return true;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").toLowerCase();
    }
}
