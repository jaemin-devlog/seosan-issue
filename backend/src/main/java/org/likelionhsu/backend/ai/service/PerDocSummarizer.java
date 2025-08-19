package org.likelionhsu.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.likelionhsu.backend.ai.dto.PerDocSummary;
import org.likelionhsu.backend.flask.FlaskSummarizeClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerDocSummarizer {

    private final FlaskSummarizeClient flask;

    @Cacheable(cacheNames = "perdoc", cacheManager = "redisCacheManager",
            key = "T(org.apache.commons.codec.digest.DigestUtils).sha256Hex(#url + '|' + (#text == null ? '' : #text))",
            unless = "#result == null || (#result.summary() == null && (#result.content() == null || #result.content().isBlank()))")
    public PerDocSummary summarizeOne(String url, String title, String sourceType, String publishedAt, String text) {
        String body = StringUtils.defaultString(text, "");

        // 본문이 너무 짧으면 LLM 호출하지 않고 바로 content로 반환
        if (body.length() < 200) {
            return new PerDocSummary(url, title, sourceType, publishedAt, null, body);
        }

        String prompt = """
                다음 본문에서 핵심 사실만 1~2문장으로 요약하라. 날짜/수치/주체가 있으면 포함하라.
                본문:
                %s
                """.formatted(StringUtils.abbreviate(body, 1200));

        String s = StringUtils.defaultIfBlank(flask.summarize(prompt), "").trim();

        // 요약이 비었으면 content로 대체
        if (s.isBlank()) {
            return new PerDocSummary(url, title, sourceType, publishedAt, null, body);
        }
        // 요약이 있으면 content는 싣지 않음(무거우면 payload 커짐)
        return new PerDocSummary(url, title, sourceType, publishedAt, s, null);
    }
}
