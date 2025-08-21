// backend/src/main/java/org/likelionhsu/backend/ai/dto/AiSearchDetailedResponse.java
package org.likelionhsu.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiSearchDetailedResponse(
        String tldr,
        List<PerDocSummary> items,
        List<String> sources
) {}
