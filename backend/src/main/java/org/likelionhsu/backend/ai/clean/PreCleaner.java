package org.likelionhsu.backend.ai.clean;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PreCleaner {

    private static final Pattern PROMPT_KEYWORDS = Pattern.compile(
            "^(역할|목표|규칙|예시|입력|출력|프롬프트)\\s*[:：]", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern FENCE_BLOCK = Pattern.compile("```[\\s\\S]*?```|\"\"\"[\\s\\S]*?\"\"\"", Pattern.MULTILINE);

    private static final Pattern HEADLINE_CLUSTER_HINT = Pattern.compile(
            "([\\[\\]\"'“”‘’«»]|\\s-\\s|…|\\|){2,}");

    private static final Set<String> SECTION_HINTS = Set.of(
            "많이 본", "관련 기사", "트렌딩", "연관 검색어", "실시간", "헤드라인", "태그", "요약:", "목록:");

    @Value("${ai.prompt.mode:strict}")
    private String promptMode;

    // 노이즈 제거 + 다주제 혼입 가능성 높은 라인 제거
    public CleanResult clean(String raw) {
        List<String> noiseFlags = new ArrayList<>();
        String text = raw == null ? "" : raw;

        // 코드/펜스 블록 제거
        var m = FENCE_BLOCK.matcher(text);
        if (m.find()) {
            text = m.replaceAll("");
            noiseFlags.add("FENCE_BLOCK_REMOVED");
        }

        // 줄 단위 필터
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // 프롬프트 지시문 패턴 제거
            if (PROMPT_KEYWORDS.matcher(trimmed).find()) {
                noiseFlags.add("PROMPT_ECHO_LINE_REMOVED");
                continue;
            }
            // 섹션/목록 힌트 제거
            boolean hasSection = SECTION_HINTS.stream().anyMatch(trimmed::contains);
            if (hasSection) {
                noiseFlags.add("SECTION_HINT_LINE_REMOVED");
                continue;
            }
            // 헤드라인 클러스터 감지(특수문자/따옴표 다량)
            if (HEADLINE_CLUSTER_HINT.matcher(trimmed).find()) {
                // 완전히 버리기보단 strict 모드에서만 제거
                if ("strict".equalsIgnoreCase(promptMode)) {
                    noiseFlags.add("HEADLINE_CLUSTER_LINE_REMOVED");
                    continue;
                }
            }
            sb.append(trimmed).append('\n');
        }

        String cleaned = sb.toString().trim();
        return new CleanResult(cleaned, noiseFlags);
    }

    public record CleanResult(String text, List<String> noiseFlags) {}
}
