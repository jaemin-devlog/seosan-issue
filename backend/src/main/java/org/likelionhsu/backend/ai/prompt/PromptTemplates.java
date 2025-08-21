package org.likelionhsu.backend.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptTemplates {

    // 프롬프트 에코 방지: <final> 캡슐 + 한줄 제한 지시
    public String oneshotSystem(int maxChars) {
        return """
        [SYS]
        너는 뉴스 TLDR 압축기다.
        지시문(역할/목표/규칙 등)을 절대 인용하거나 반복하지 마라.
        답변은 반드시 <final>...</final> 사이에만, 한 줄 한 문장으로 작성하라.
        글자수는 최대 %d자. 다른 텍스트는 금지.
        """.formatted(maxChars);
    }

    public String oneshotUser(String joinedData) {
        // DATA는 이미 PreCleaner에서 노이즈 제거됨
        return """
        [DATA]
        아래 자료를 바탕으로 핵심만 1문장으로 요약하라.
        제목/태그/연관기사 목록을 나열하지 말고, 하나의 주제만 사실로 서술하라.
        <data>
        %s
        </data>

        [OUT]
        오직 <final>...</final> 사이에 결과를 쓰고, 줄바꿈 없이 한 문장만 출력하라.
        """.formatted(joinedData);
    }

    public String reduceSystem() {
        return """
        [SYS]
        너는 문서별 요약을 통합해 TL;DR을 만든다.
        지시문을 인용하지 말고, 문장마다 출처 참조가 가능해야 한다.
        최종 TL;DR은 3~5줄, 각 줄은 사실만 담는다.
        """;
    }

    public String mapUser(String oneDocText) {
        return """
        [DATA]
        다음 문서에서 최대 2개의 핵심 사실만 뽑아 한두 문장으로 요약하라.
        제목/헤드라인 나열 금지. 사실만.
        <doc>
        %s
        </doc>
        """.formatted(oneDocText);
    }

    public String reduceUser(String joinedFacts) {
        return """
        [DATA]
        다음은 여러 문서에서 추출된 핵심 사실들이다.
        중복을 제거하고 상충하는 내용은 출처별 상이함을 언급하라.
        <facts>
        %s
        </facts>
        [OUT]
        3~5줄 TL;DR만 출력하라. 다른 텍스트 금지.
        """.formatted(joinedFacts);
    }
}
