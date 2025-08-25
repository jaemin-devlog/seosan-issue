package org.likelionhsu.backend.ai.filter;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class ContentQualityGate {

    private ContentQualityGate() {}

    // 기존 기준(현재 코드에 맞춤)
    private static final int MIN_LEN_DEFAULT = 380;
    private static final int MIN_LEN_UGC     = 140;  // 블로그/카페 완화
    private static final int NOISE_THRESHOLD = 50;

    public enum FailReason { OK, TOO_SHORT, MISSING_TERMS, NOISE }

    /** 기존 호출부 호환용 */
    public static boolean pass(String title, String body, Set<String> mustTerms) {
        return pass(title, body, mustTerms, null);
    }

    /** 기존 호출부 호환용 (ExploreSummarizeService가 호출) */
    public static boolean pass(String title, String body, Set<String> mustTerms, String host) {
        return check(title, body, mustTerms, host) == FailReason.OK;
    }

    /** 디버그용: 왜 탈락했는지 리턴 */
    public static FailReason check(String title, String body, Set<String> mustTerms, String host) {
        String t = norm(title);
        String b = norm(body);

        if (b.isEmpty() && t.isEmpty()) return FailReason.TOO_SHORT;

        boolean isUGC = host != null && (
                host.contains("blog.naver.com") || host.contains("m.blog.naver.com")
                        || host.contains("tistory.com")   || host.contains("cafe.naver.com")
        );

        String candidate = !b.isEmpty() ? b : t;

        int minLen = isUGC ? MIN_LEN_UGC : MIN_LEN_DEFAULT;
        if (candidate.length() < minLen) return FailReason.TOO_SHORT;

        // 키워드 체크: mustTerms가 있고, 1개도 안 맞으면 실패
        if (mustTerms != null && !mustTerms.isEmpty()) {
            if (!containsAny(candidate, mustTerms) && !containsAny(t, mustTerms)) {
                return FailReason.MISSING_TERMS;
            }
        }

        // 노이즈 체크
        int noise = candidate.replaceAll("[가-힣a-zA-Z0-9\\s.,:;()\\-]", "").length();
        if (noise > NOISE_THRESHOLD) return FailReason.NOISE;

        return FailReason.OK;
    }

    /* ---------------- helpers ---------------- */

    private static String norm(String s) {
        if (s == null) return "";
        // 한국어에서도 안정적인 포함 체크를 위해 NFKC + 소문자 + 공백 정리
        String x = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        return x;
    }

    private static boolean containsAny(String haystack, Set<String> needles) {
        if (haystack == null || haystack.isBlank()) return false;
        String h = haystack;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            String nn = Normalizer.normalize(n, Normalizer.Form.NFKC)
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            if (!nn.isEmpty() && h.contains(nn)) return true;
        }
        return false;
    }
}
