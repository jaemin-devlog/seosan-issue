package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PerDocSummarizer {

    private final FlaskSummarizeClient flask;

    public PerDocSummary summarizeOne(String url, String title, String sourceType,
                                      String publishedAt, String body) {
        String safeTitle = safe(title);
        String safeBody  = safe(body);

        // 본문이 짧으면 제목으로 폴백
        String input = StringUtils.isNotBlank(safeBody) ? safeBody : safeTitle;

        // ✅ 요약 전 전처리(괄호 블럭/기자/날짜/이메일/광고 꼬리 제거)
        String cleaned = preClean(input);

        String summary;
        try {
            summary = flask.summarizeText(cleaned)
                    .blockOptional()
                    .orElse("")
                    .trim();
        } catch (Exception e) {
            summary = "";
        }
        summary = postClean(sanitizeResponse(summary));

        if (StringUtils.isBlank(summary)) summary = null;

        return new PerDocSummary(
                url,
                safeTitle,
                sourceType,
                publishedAt,
                summary,
                StringUtils.isBlank(safeBody) ? safeTitle : safeBody
        );
    }

    /* ----------------- helpers ----------------- */

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /** 허용 문자만 남기기 */
    private static String sanitizeResponse(String input) {
        if (input == null) return "";
        return input.replaceAll("[^가-힣a-zA-Z0-9 .,!]", "").trim();
    }

    /** 간단 후처리: 중복 토큰/공백 정리 */
    private static String postClean(String s) {
        if (s == null) return "";
        String x = s.replaceAll("\\s+", " ").trim();
        // 같은 짧은 토큰 3회 이상 반복될 때 1회로 축약
        x = x.replaceAll("([가-힣A-Za-z0-9]{1,3})(\\s+\\1){2,}", "$1");
        return x;
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

        // 1) 대괄호/소괄호 안의 짤막한 블록 제거 (사진/제공/지역표기 등)
        x = x.replaceAll("\\([^)]{1,80}\\)", " ");  // ( ... ) 최대 80자
        x = x.replaceAll("\\[[^\\]]{1,80}\\]", " "); // [ ... ] 최대 80자

        // 2) 반복적으로 등장하는 저작권/광고 꼬리 제거
        x = x.replaceAll("무단\\s*전재\\s*및\\s*재배포\\s*금지.*", " ");
        x = x.replaceAll("재판매\\s*및\\s*DB\\s*금지.*", " ");
        x = x.replaceAll("기사\\s*제보.*", " ");
        x = x.replaceAll("카카오톡\\s*:\\s*.*", " ");
        x = x.replaceAll("네이버\\s*메인.*", " ");
        x = x.replaceAll("AD\\b.*", " ");
        x = x.replaceAll("^HOME\\s*>.*", " ");

        // 3) 기자/이메일/연락처 및 입력·수정 일시 제거
        x = x.replaceAll("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}", " ");
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
