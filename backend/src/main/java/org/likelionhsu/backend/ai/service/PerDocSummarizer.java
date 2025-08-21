package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PerDocSummarizer {

    private final FlaskSummarizeClient flask;

    // 프롬프트/펜스 블록 제거용 간단한 패턴 (프롬프트 에코 방지)
    private static final Pattern FENCE_BLOCK = Pattern.compile("```[\\s\\S]*?```|\"\"\"[\\s\\S]*?\"\"\"", Pattern.MULTILINE);
    private static final Pattern PROMPT_LINE = Pattern.compile("^(역할|목표|규칙|예시|입력|출력|프롬프트)\\s*[:：].*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);

    @Cacheable(cacheNames = "perdoc", cacheManager = "redisCacheManager",
            key = "T(org.apache.commons.codec.digest.DigestUtils).sha256Hex(#url + '|' + (#text == null ? '' : #text))",
            unless = "#result == null || (#result.summary() == null && (#result.content() == null || #result.content().isBlank()))")
    public PerDocSummary summarizeOne(String url, String title, String sourceType, String publishedAt, String text) {
        String body = StringUtils.defaultString(text, "");

        // 본문이 너무 짧으면 LLM 호출하지 않고 바로 content로 반환
        if (body.length() < 200) {
            return new PerDocSummary(url, title, sourceType, publishedAt, null, body);
        }

        // --- 경량 Pre-clean: 펜스 블록/프롬프트 지시문 라인 제거 ---
        String cleaned = FENCE_BLOCK.matcher(body).replaceAll("");
        cleaned = PROMPT_LINE.matcher(cleaned).replaceAll("");
        cleaned = cleaned.trim();

        // 요약 입력 길이 제한 (너무 길면 노이즈 유입 ↑)
        String clipped = StringUtils.abbreviate(cleaned, 1200);

        // system/user 분리 호출
        String system = """
                [SYS]
                너는 문서별 요약기다.
                지시문(역할/목표/규칙 등)을 인용하지 말고, 사실만 1~2문장으로 요약하라.
                날짜/수치/주체가 있으면 포함하라.
                출력은 문장형 텍스트만.
                """;

        String user = """
                [DATA]
                다음 문서를 요약하라.
                <doc>
                %s
                </doc>
                """.formatted(clipped);

        // Flask 클라이언트는 system, user를 받도록 수정되어 있음
        String s = flask.summarize(system, user)
                .blockOptional()        // Mono<String> -> Optional<String>
                .map(String::trim)      // 앞뒤 공백 제거
                .orElse("");            // null/빈 Optional이면 빈 문자열

        // 요약이 비었으면 content로 대체
        if (s.isEmpty()) {
            return new PerDocSummary(url, title, sourceType, publishedAt, null, body);
        }
        // 요약이 있으면 content는 싣지 않음(페이로드 경량화)
        return new PerDocSummary(url, title, sourceType, publishedAt, s, null);
    }
}
