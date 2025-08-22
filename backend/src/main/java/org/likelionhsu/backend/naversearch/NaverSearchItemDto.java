package org.likelionhsu.backend.naversearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaverSearchItemDto {

    private String title;
    private String description;

    /** 네이버 중계 링크 */
    private String link;

    /** 언론사 원문 링크 (뉴스에서만 내려옴) */
    @JsonProperty("originallink")
    private String originallink;

    /** 선택: 타입(“news”, “blog”, …)을 쓰고 있다면 유지 */
    private String type;

    /** 선택: 응답에 따라 존재하는 필드 */
    private String bloggername; // blog
    private String postdate;    // blog (YYYYMMDD)
    private String pubDate;     // news (RFC822)


    public NaverSearchItemDto(String title, String description, String link, String originallink, String type) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.originallink = originallink;
        this.type = type;
    }
}
