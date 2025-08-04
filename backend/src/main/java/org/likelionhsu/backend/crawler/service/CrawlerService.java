package org.likelionhsu.backend.crawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelionhsu.backend.crawler.dto.CrawlResponseDto;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final PostRepository postRepository;
    private final WebClient webClient;

    @Value("http://seosan-crawler-api:5001")
    private String crawlerApiBaseUrl;

    // 대상 게시판 정보
    private static final List<BoardInfo> boards = List.of(
            new BoardInfo("시정소식", "https://www.seosan.go.kr/seosan/bbs/B0000039/list.do?pageIndex="),
            new BoardInfo("고시공고", "https://www.seosan.go.kr/seosan/bbs/B0000040/list.do?pageIndex="),
            new BoardInfo("타기관소식", "https://www.seosan.go.kr/seosan/bbs/B0000041/list.do?pageIndex="),
            new BoardInfo("입법예고", "https://www.seosan.go.kr/seosan/bbs/B0000042/list.do?pageIndex="),
            new BoardInfo("보도자료", "https://www.seosan.go.kr/seosan/bbs/B0000043/list.do?pageIndex="),
            new BoardInfo("공지사항", "https://www.seosan.go.kr/seosan/bbs/B0000044/list.do?pageIndex=")
    );

    // 3시간마다 실행 (fixedRate = 10800000 ms)
    @Scheduled(fixedRate = 10800000)
    @Transactional
    public void scheduleCrawling() {
        log.info("========= 서산시청 게시판 크롤링 시작 =========");
        boards.forEach(this::crawlAndSave);
        log.info("========= 서산시청 게시판 크롤링 종료 =========");
    }

    @PostConstruct
    public void init() {
        // 애플리케이션 시작 시 크롤링 1회 실행
        scheduleCrawling();
    }

    private void crawlAndSave(BoardInfo board) {
        log.info("{} 크롤링 중... URL: {}", board.category, board.url);
        try {
            CrawlResponseDto response = webClient.get()
                    .uri(crawlerApiBaseUrl, uriBuilder -> uriBuilder
                            .path("/crawl")
                            .queryParam("category", board.category)
                            .queryParam("url", board.url)
                            .build())
                    .retrieve()
                    .bodyToMono(CrawlResponseDto.class)
                    .block(); // 비동기 결과를 동기적으로 기다림

            if (response != null && response.getPosts() != null && !response.getPosts().isEmpty()) {
                saveCrawledPosts(response);
                log.info("{} 크롤링 완료. {}개의 새로운 게시글 저장.", board.category, response.getPostCount());
            } else {
                log.info("{} 새로운 게시글 없음.", board.category);
            }
        } catch (Exception e) {
            log.error("{} 크롤링 중 에러 발생: {}", board.category, e.getMessage());
        }
    }

    private void saveCrawledPosts(CrawlResponseDto response) {
        List<Post> postsToSave = response.getPosts().stream()
                .map(postData -> {
                    Integer views = null;
                    try {
                        views = Integer.parseInt(postData.getViews());
                    } catch (NumberFormatException e) {
                        // "-" 와 같은 숫자가 아닌 값일 경우 null로 처리
                        views = 0;
                    }

                    return Post.builder()
                            .title(postData.getTitle())
                            .content(postData.getContent())
                            .link(postData.getLink())
                            .pubDate(postData.getDate())
                            .region("서산시 전체") // 크롤링 대상은 서산시 전체로 간주
                            .category(Category.fromValue(response.getCategory()))
                            .department(postData.getDepartment())
                            .views(views)
                            .crawledAt(LocalDateTime.now())
                            .build();
                })
                .toList();

        postRepository.saveAll(postsToSave);
    }

    // 내부 클래스로 게시판 정보 관리
    private record BoardInfo(String category, String url) {}
}
