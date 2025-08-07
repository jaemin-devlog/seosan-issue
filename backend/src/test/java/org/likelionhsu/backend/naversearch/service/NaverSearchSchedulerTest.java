package org.likelionhsu.backend.naversearch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.backend.common.config.NaverSearchProperties;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.service.PostService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaverSearchSchedulerTest {

    @Mock
    private NaverSearchService naverSearchService;

    @Mock
    private PostService postService;

    @Mock
    private NaverSearchProperties naverSearchProperties;

    @InjectMocks
    private NaverSearchScheduler naverSearchScheduler;

    private Post mockPost1;
    private Post mockPost2;

    @BeforeEach
    void setUp() {
        mockPost1 = Post.builder()
                .title("스케줄러 테스트 포스트 1")
                .content("내용 1")
                .link("http://link1.com")
                .pubDate("2023-01-01")
                .region("서산시")
                .category(Category.BLOG)
                .crawledAt(LocalDateTime.now())
                .build();
        mockPost2 = Post.builder()
                .title("스케줄러 테스트 포스트 2")
                .content("내용 2")
                .link("http://link2.com")
                .pubDate("2023-01-02")
                .region("태안군")
                .category(Category.CAFE)
                .crawledAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("네이버 검색 스케줄러 실행 - 검색 결과 있음")
    void scheduleNaverSearch_withResults() {
        // Given
        List<String> queries = Arrays.asList("맛집", "여행");
        List<String> regions = Arrays.asList("서산시", "태안군");

        when(naverSearchProperties.getQueries()).thenReturn(queries);
        when(naverSearchProperties.getRegions()).thenReturn(regions);

        // Mock searchNaverBlogsAndCafes to return posts
        when(naverSearchService.searchNaverBlogsAndCafes(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(Arrays.asList(mockPost1, mockPost2));

        // When
        naverSearchScheduler.scheduleNaverSearch();

        // Then
        // 2 regions * 2 queries = 4 calls to searchNaverBlogsAndCafes
        verify(naverSearchService, times(4)).searchNaverBlogsAndCafes(anyString(), anyInt(), anyInt(), anyString());
        // postService.savePosts should be called once with all collected posts
        verify(postService, times(1)).savePosts(any(List.class));
    }

    @Test
    @DisplayName("네이버 검색 스케줄러 실행 - 검색 결과 없음")
    void scheduleNaverSearch_noResults() {
        // Given
        List<String> queries = Arrays.asList("맛집");
        List<String> regions = Arrays.asList("서산시");

        when(naverSearchProperties.getQueries()).thenReturn(queries);
        when(naverSearchProperties.getRegions()).thenReturn(regions);

        // Mock searchNaverBlogsAndCafes to return empty list
        when(naverSearchService.searchNaverBlogsAndCafes(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());

        // When
        naverSearchScheduler.scheduleNaverSearch();

        // Then
        verify(naverSearchService, times(1)).searchNaverBlogsAndCafes(anyString(), anyInt(), anyInt(), anyString());
        // postService.savePosts should NOT be called
        verify(postService, times(0)).savePosts(any(List.class));
    }
}
