package org.likelionhsu.backend.crawler.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CrawlResponseDto {
    private String category;
    private int postCount;
    private List<PostData> posts;

    @Getter
    @NoArgsConstructor
    public static class PostData {
        private int id;
        private String title;
        private String link;
        private String content;
        private String attachment;
        private String views;
        private String date;
        private String department;
    }
}
