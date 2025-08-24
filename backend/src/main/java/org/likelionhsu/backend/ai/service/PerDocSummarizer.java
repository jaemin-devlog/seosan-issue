package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PerDocSummarizer {

    private final FlaskSummarizeClient flask;

    /** 요약 스킵 임계 길이 (이하이면 요약 건너뛰고 원문 반환) */
    @Value("${ai.summarize.min-length-threshold:280}")
    private int minLenThreshold;

    /** 요약/원문 클립 상한 */
    @Value("${ai.summarize.max-output-length:600}")
    private int maxOutputLength;

    /** system/user/assistant 등 프롬프트 누수 라인 제거 */
    private static final Pattern PROMPT_LEAK_LINES =
            Pattern.compile("(?im)^\\s*(system|user|assistant)\\s*[:：].*$");

    /** ‘as an ai language model’ 류 문장 제거 */
    private static final Pattern AILM_LINE =
            Pattern.compile("(?im)^.*\\bas an ai language model\\b.*$");

    /** 연속 문자 반복(가가가/주주주/aaaa) 축소: 3회 이상 → 2회 */
    private static final Pattern REPEATED_CHAR =
            Pattern.compile("([\\p{IsHangul}A-Za-z0-9])\\1{2,}");

    public PerDocSummary summarizeOne(String url, String title, String sourceType,
                                      String publishedAt, String body) {

        final String safeTitle = safe(title);
        final String safeBody  = safe(body);

        // 본문이 짧으면 제목으로 폴백용 입력 구성
        final String candidateInput = StringUtils.isNotBlank(safeBody) ? safeBody : safeTitle;

        // 요약 전 전처리(괄호/기자/날짜/메일/광고 꼬리 등 제거)
        final String cleanedInput = preClean(candidateInput);

        String summary;
        boolean bypassed = false;

        // ✅ 1) 짧은 글: 요약 스킵하고 "본문 그대로(클린 + 클립)" 반환
        if (effectiveLength(cleanedInput) <= Math.max(0, minLenThreshold)) {
            summary = clip(postClean(sanitizeResponse(cleanedInput)), maxOutputLength);
            bypassed = true;
        } else {
            // ✅ 2) 긴 글: Flask 요약 → 후처리 → 클립
            try {
                summary = flask.summarizeText(cleanedInput)
                        .blockOptional() // Mono<String> 대응
                        .orElse("")
                        .trim();
            } catch (Exception e) {
                summary = "";
            }
            summary = clip(postClean(sanitizeResponse(summary)), maxOutputLength);
            if (StringUtils.isBlank(summary)) {
                // 요약 실패/빈 응답시 폴백: 원문을 바로 사용
                summary = clip(postClean(sanitizeResponse(cleanedInput)), maxOutputLength);
            }
        }

        // PerDocSummary는 record → 필드 추가 없이 그대로 반환
        // summary: (짧은 글) 원문 그대로 / (긴 글) 요약
        // content: 원문(본문이 있으면 본문, 없으면 제목)
        return new PerDocSummary(
                url,
                safeTitle,
                sourceType,
                safe(publishedAt),
                summary,
                StringUtils.isBlank(safeBody) ? safeTitle : safeBody
        );
    }

    /* ----------------- helpers ----------------- */

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /** 허용 문자만 남기기 (HTML 꺾쇠 등 제거 효과) */
    private static String sanitizeResponse(String input) {
        if (input == null) return "";
        // 한/영/숫자/기본 구두점만 허용
        return input.replaceAll("[^가-힣a-zA-Z0-9 .,!?;:'\"()\\-/_\\n]", "").trim();
    }

    /** 결과 후처리: 프롬프트 누수 라인 제거 + 반복 축소 + 공백 정리 */
    private String postClean(String s) {
        if (s == null) return "";
        String x = s;

        // 줄 단위로 system/user/assistant: … 제거
        x = PROMPT_LEAK_LINES.matcher(x).replaceAll("");
        // ‘as an ai language model’ 라인 제거
        x = AILM_LINE.matcher(x).replaceAll("");

        // 동일 문자 3회 이상 반복 → 2회로 축소 (가가가 → 가가, 주주주 → 주주)
        x = REPEATED_CHAR.matcher(x).replaceAll("$1$1");

        // 같은 짧은 토큰 3회 이상 반복(공백 구분) → 1회로 축약
        x = x.replaceAll("([가-힣A-Za-z0-9]{1,3})(\\s+\\1){2,}", "$1");

        // 여러 공백/개행 정리
        x = x.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("(?m)^[ \\t]+", "")
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("(?m)^\\s*$\\n?", "")
                .trim();

        return x;
    }

    /** 입력 길이(공백 정리 후) */
    private static int effectiveLength(String s) {
        if (s == null) return 0;
        return s.replaceAll("\\s+", " ").trim().length();
    }

    /** 최대 길이로 잘라내기 */
    private String clip(String s, int limit) {
        if (s == null) return "";
        if (limit <= 0 || s.length() <= limit) return s;
        return s.substring(0, Math.max(0, limit)).trim() + " …";
    }

    /**
     * 기사 본문 전처리:
     * - ()/[] 안의 짤막한 캡션·출처·기자 표기 제거(최대 80자)
     * - “재판매 및 DB 금지/무단 전재 및 재배포 금지/사진=” 등 꼬리 제거
     * - 기자/이메일/입력·수정 일시/광고·CTA 라인 제거
     */
    private static String preClean(String s) {
        if (s == null) return "";
        String x = s;

        // 1) 괄호 안의 짤막한 캡션/출처/기자 표기 제거
        x = x.replaceAll("\\([^)]{1,80}\\)", " ");
        x = x.replaceAll("\\[[^]]{1,80}\\]", " ");

        // 2) 법적 고지/재배포 금지/광고성 꼬리
        x = x.replaceAll("재판매 및 DB 금지", " ");
        x = x.replaceAll("무단[\\s-]*전재[\\s-]*및[\\s-]*재배포[\\s-]*금지", " ");
        x = x.replaceAll("광고\\s*문의.*", " ");
        x = x.replaceAll("바로가기|클릭하세요|상세보기|자세히 보기", " ");

        // 3) 기자/이메일/입력·수정 일시
        x = x.replaceAll("\\b[가-힣]{2,5}\\s*기자\\b", " ");
        x = x.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+", " ");
        x = x.replaceAll("\\b(입력|수정)\\s*[:：]?\\s*\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}(\\s*\\d{1,2}:\\d{2})?\\b", " ");
        x = x.replaceAll("\\b\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}(\\s*\\d{1,2}:\\d{2})?\\b", " ");
        x = x.replaceAll("\\b[가-힣]{2,5}\\s*기자\\b.*", " ");

        // 4) ‘사진=’ ‘제공=’ 같은 캡션 꼬리
        x = x.replaceAll("사진\\s*=\\s*[^\\s]+", " ");
        x = x.replaceAll("제공\\s*=\\s*[^\\s]+", " ");

        // 5) 공백 정리
        x = x.replaceAll("\\s+", " ").trim();

        return x;
    }
}
