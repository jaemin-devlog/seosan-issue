package org.likelionhsu.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleText {
    private String url;
    private String title;
    private String byline;
    private String text; // 정제 텍스트
}
