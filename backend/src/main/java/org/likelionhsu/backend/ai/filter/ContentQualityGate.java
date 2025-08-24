package org.likelionhsu.backend.ai.filter;

import java.util.Set;

public final class ContentQualityGate {

    private static final int MIN_LEN_DEFAULT = 380;
    private static final int MIN_LEN_UGC     = 140; // ✅ 블로그/카페 완화 (기존 260 → 140)
    private static final int NOISE_THRESHOLD = 50;  // 비정상 기호 밀집 컷

    private ContentQualityGate() {}

    public static boolean pass(String title, String body, Set<String> mustTerms) {
        return pass(title, body, mustTerms, null);
    }

    public static boolean pass(String title, String body, Set<String> mustTerms, String host) {
        if (body == null) return false;

        final boolean isUGC = host != null &&
                (host.contains("blog.naver.com") || host.contains("m.blog.naver.com")
                        || host.contains("tistory.com") || host.contains("cafe.naver.com"));

        String t = normalize(title);
        String b = normalize(body);

        // 길이 기준(UGC 더 완화)
        int minLen = isUGC ? MIN_LEN_UGC : MIN_LEN_DEFAULT;
        if (b.length() < minLen) return false;

        // 키워드 포함 기준(있다면 1개 이상 포함)
        if (mustTerms != null && !mustTerms.isEmpty()) {
            long hits = mustTerms.stream().filter(term -> t.contains(term) || b.contains(term)).count();
            if (hits < 1) return false;
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
