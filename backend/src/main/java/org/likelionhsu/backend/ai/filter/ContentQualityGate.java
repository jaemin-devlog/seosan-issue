package org.likelionhsu.backend.ai.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ContentQualityGate {

    /* ===== 인스턴스 설정 (신규 파이프라인에서 사용) ===== */

    @Value("${ai.summarize.min-length-threshold:280}")
    private int minLengthThreshold;

    @Value("${ai.summarize.enable-post-cleanup:true}")
    private boolean enablePostCleanup;

    private static final List<Pattern> PROMPT_LEAK_PATTERNS = List.of(
            Pattern.compile("(?im)^\\s*(system|user|assistant)\\s*[:：]\\s*.*$"),
            Pattern.compile("(?im)^\\s*you are chatgpt.*$"),
            Pattern.compile("(?im)^\\s*as an ai language model.*$")
    );

    private static final Pattern REPEATED_CHAR = Pattern.compile("([\\p{IsHangul}\\p{L}\\p{N}])\\1{2,}");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]{1,200}>");

    public boolean shouldBypassSummarization(String text) {
        if (text == null) return true;
        String normalized = normalize(text);
        int effectiveLen = normalized.strip().length();
        return effectiveLen > 0 && effectiveLen <= minLengthThreshold;
    }

    /** 요약 입력 전 정규화 (NFC) */
    public String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /** 요약 결과 후처리 (프롬프트/노이즈 제거, 과도 반복 축소) */
    public String cleanupSummary(String summary) {
        if (!enablePostCleanup || summary == null || summary.isBlank()) return summary;

        String s = summary;

        // HTML/태그 제거(과한 제거 방지: 200자 이하 태그만)
        s = HTML_TAG.matcher(s).replaceAll("");

        // 프롬프트 흔적 줄 단위 제거
        for (Pattern p : PROMPT_LEAK_PATTERNS) {
            s = p.matcher(s).replaceAll("").trim();
        }

        // 한/영/숫자 문자 3회 이상 반복 → 2회로 축소 (가가가 → 가가)
        s = REPEATED_CHAR.matcher(s).replaceAll("$1$1");

        // 공백 정리
        s = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("(?m)^[ \\t]+", "")
                .replaceAll("(?m)[ \\t]+$", "")
                .trim();

        return s;
    }

    /* ===== 레거시 호환용 정적 API (ExploreSummarizeService가 호출) =====
       ExploreSummarizeService.java:44 -> ContentQualityGate.pass(title, body, terms, host)
       아래 메서드는 의존성 없이 동작하도록 보수적으로 구현했습니다.
    */

    private static final int STATIC_MIN_LEN = 40; // 너무 짧은 본문은 탈락

    /**
     * 레거시 품질필터: 제목/본문/키워드로 최소 품질 검증
     * @param title   제목
     * @param body    본문
     * @param terms   검색 키워드 세트(있으면 제목/본문에 하나 이상 포함되어야 통과)
     * @param host    호스트(현재 버전에서는 차단 도메인 판정 안 함)
     * @return        true = 통과(요약 대상으로 사용 가능)
     */
    public static boolean pass(String title, String body, Set<String> terms, String host) {
        String t = safe(title);
        String b = safe(body);

        // 제목/본문 둘 다 비었으면 실패
        if (t.isEmpty() && b.isEmpty()) return false;

        // 본문이 있으면 본문, 없으면 제목으로 길이 판단
        String candidate = !b.isEmpty() ? b : t;

        // 최소 길이 미만이면 실패 (너무 짧아 노이즈/프롬프트 혼입 가능)
        if (effectiveLength(candidate) < STATIC_MIN_LEN) return false;

        // 키워드가 주어졌다면 제목/본문 중 최소 1개 이상 포함해야 함
        if (terms != null && !terms.isEmpty()) {
            if (!containsAny(candidate, terms) && !containsAny(t, terms)) {
                return false;
            }
        }

        // host 기반 차단/허용 정책은 레거시 정적 메서드에선 건너뜀(Null-safe)
        return true;
    }

    /* ===== 정적 헬퍼(레거시 pass에서 사용) ===== */

    private static String safe(String s) {
        if (s == null) return "";
        // 공백 정리만 수행(HTML 제거까지는 안 함)
        return s.replaceAll("\\s+", " ").trim();
    }

    private static int effectiveLength(String s) {
        if (s == null) return 0;
        return s.replaceAll("\\s+", " ").trim().length();
    }

    private static boolean containsAny(String haystack, Set<String> needles) {
        if (haystack == null || haystack.isBlank()) return false;
        String h = haystack.toLowerCase(Locale.ROOT);
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (h.contains(n.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
