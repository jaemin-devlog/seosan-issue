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
            unless = "#result == null || #result.summary() == null || #result.summary().isBlank()")
    public PerDocSummary summarizeOne(String url, String title, String sourceType, String publishedAt, String text) {
        String body = StringUtils.defaultString(text, "");
        String prompt = """
                다음 본문에서 핵심 사실만 1~2문장으로 요약하라. 날짜/수치/주체가 있으면 포함하라.
                본문:
                %s
                """.formatted(StringUtils.abbreviate(body, 1200));

        String s = flask.summarize(prompt);
        return new PerDocSummary(url, title, sourceType, publishedAt, s);
    }
}
