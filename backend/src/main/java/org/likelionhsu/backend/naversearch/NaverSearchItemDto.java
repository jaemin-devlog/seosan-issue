package org.likelionhsu.backend.naversearch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaverSearchItemDto {
    private String title;       // HTML 태그 포함 (프론트에서 제거)
    private String description; // 잘린 스니펫
    private String link;        // 원문 링크
    private String type;        // news|blog|cafearticle|webkr
}
