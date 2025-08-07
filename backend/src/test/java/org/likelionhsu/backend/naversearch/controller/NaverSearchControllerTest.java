package org.likelionhsu.backend.naversearch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.likelionhsu.backend.naversearch.service.NaverSearchService;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NaverSearchController.class)
class NaverSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NaverSearchService naverSearchService;

    @MockBean
    private PostService postService;

    @Test
    @DisplayName("네이버 블로그/카페 검색 및 저장 성공")
    void searchNaverBlogsAndCafes_success() throws Exception {
        // Given
        String query = "서산 맛집";
        int display = 10;
        int start = 1;

        Post mockPost1 = Post.builder()
                .title("서산 맛집 1")
                .content("내용 1")
                .link("http://link1.com")
                .pubDate("2023-01-01")
                .region("서산시")
                .category(Category.BLOG)
                .crawledAt(LocalDateTime.now())
                .build();
        Post mockPost2 = Post.builder()
                .title("서산 맛집 2")
                .content("내용 2")
                .link("http://link2.com")
                .pubDate("2023-01-02")
                .region("서산시")
                .category(Category.CAFE)
                .crawledAt(LocalDateTime.now())
                .build();
        List<Post> mockPosts = Arrays.asList(mockPost1, mockPost2);

        when(naverSearchService.searchNaverBlogsAndCafes(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(mockPosts);

        // When & Then
        mockMvc.perform(get("/api/v1/naver-search/blogs-cafes")
                        .param("query", query)
                        .param("display", String.valueOf(display))
                        .param("start", String.valueOf(start)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("서산 맛집 1"));

        // Verify that postService.savePosts was called
        verify(postService, times(1)).savePosts(mockPosts);
    }

    @Test
    @DisplayName("네이버 블로그/카페 검색 결과 없음")
    void searchNaverBlogsAndCafes_noResult() throws Exception {
        // Given
        String query = "없는 검색어";
        int display = 10;
        int start = 1;

        when(naverSearchService.searchNaverBlogsAndCafes(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/naver-search/blogs-cafes")
                        .param("query", query)
                        .param("display", String.valueOf(display))
                        .param("start", String.valueOf(start)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Verify that postService.savePosts was NOT called
        verify(postService, times(0)).savePosts(any());
    }
}
