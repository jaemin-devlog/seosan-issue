package org.likelionhsu.backend.naversearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.common.config.NaverSearchProperties;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.service.PostService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NaverSearchScheduler {

    private final NaverSearchService naverSearchService;
    private final PostService postService;
    private final NaverSearchProperties naverSearchProperties;

    // 매일 자정(0시 0분 0초)에 실행
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleNaverSearch() {
        log.info("네이버 검색 스케줄러 시작");

        List<String> queries = naverSearchProperties.getQueries();
        List<String> regions = naverSearchProperties.getRegions();

        List<Post> allCollectedPosts = new ArrayList<>();

        for (String region : regions) {
            for (String query : queries) {
                String fullQuery = region.equals("서산시 전체") ? query : region + " " + query; // 읍면동 + 쿼리 또는 쿼리만
                log.info("검색 시작: 지역 = {}, 쿼리 = {}", region, fullQuery);

                // 블로그/카페 검색
                List<Post> blogCafePosts = naverSearchService.searchNaverBlogsAndCafes(fullQuery, 100, 1, region);
                allCollectedPosts.addAll(blogCafePosts);
            }
        }

        if (!allCollectedPosts.isEmpty()) {
            postService.savePosts(allCollectedPosts);
            log.info("총 {}개의 네이버 검색 게시물 저장 완료.", allCollectedPosts.size());
        } else {
            log.info("네이버 검색 결과 없음.");
        }
        log.info("네이버 검색 스케줄러 종료");
    }
}
