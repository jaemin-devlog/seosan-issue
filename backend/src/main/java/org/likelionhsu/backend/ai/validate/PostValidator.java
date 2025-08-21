package org.likelionhsu.backend.ai.validate;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostValidator {

    @Value("${ai.prompt.one-line-tldr-chars:28}")
    private int oneLineMax;

    @Value("${ai.prompt.banned-phrases:}")
    private List<String> bannedPhrases;

    public ValidationResult validateOneLine(String modelOutput) {
        String raw = modelOutput == null ? "" : modelOutput.trim();

        // <final> 캡슐만 허용
        int start = raw.indexOf("<final>");
        int end = raw.indexOf("</final>");
        if (start < 0 || end < 0 || end <= start) {
            return ValidationResult.fail("FINAL_TAG_MISSING");
        }
        String inner = raw.substring(start + 7, end).trim();

        // 한 줄 한 문장
        if (inner.contains("\n") || inner.contains("\r")) {
            return ValidationResult.fail("MULTILINE_OUTPUT");
        }
        // 길이 제한
        if (inner.length() > oneLineMax) {
            return ValidationResult.fail("OVER_LENGTH");
        }
        // 금칙어/프롬프트 에코 흔적
        for (String ban : bannedPhrases) {
            if (inner.contains(ban)) {
                return ValidationResult.fail("PROMPT_ECHO_DETECTED");
            }
        }
        // 콜론/펜스/삼중따옴표 금지
        if (inner.contains(":") || inner.contains("```") || inner.contains("\"\"\"")) {
            return ValidationResult.fail("FORMAT_FORBIDDEN_TOKENS");
        }

        return ValidationResult.ok(inner);
    }

    public record ValidationResult(boolean ok, String reason, String content) {
        public static ValidationResult ok(String content) { return new ValidationResult(true, null, content); }
        public static ValidationResult fail(String reason) { return new ValidationResult(false, reason, null); }
    }
}
