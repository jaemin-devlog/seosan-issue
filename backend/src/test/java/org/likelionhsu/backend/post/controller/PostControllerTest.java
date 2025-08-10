package org.likelionhsu.backend.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.common.exception.customexception.PostCustomException;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.dto.response.PostDetailResponseDto;
import org.likelionhsu.backend.post.dto.response.PostResponseDto;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @Test
    @DisplayName("게시글 ID로 상세 조회 성공")
    void getPostById_success() throws Exception {
        // Given
        Long postId = 1L;
        PostDetailResponseDto mockResponse = PostDetailResponseDto.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .link("http://test.com")
                .pubDate("2023-01-01")
                .region("서산시")
                .category(Category.NOTICE) // String 대신 Category enum 직접 사용
                .crawledAt(LocalDateTime.now())
                .build();

        when(postService.findPostById(anyLong())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value("Test Title"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 ID로 상세 조회 시 404 에러")
    void getPostById_notFound() throws Exception {
        // Given
        Long postId = 999L;
        when(postService.findPostById(anyLong()))
                .thenThrow(new PostCustomException(ErrorCode.POST_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.POST_NOT_FOUND.getStatus().value())) // getCode() 대신 getStatus().value() 사용
                .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("필터 조건으로 게시글 목록 조회 성공")
    void getFilteredPosts_successWithFilters() throws Exception {
        // Given
        String region = "서산시";
        Category category = Category.NOTICE;
        List<PostResponseDto> mockResponseList = Arrays.asList(
                PostResponseDto.builder()
                        .id(1L)
                        .title("Filtered Post 1")
                        .pubDate("2023-01-01")
                        .region(region)
                        .category(category)
                        .build(),
                PostResponseDto.builder()
                        .id(2L)
                        .title("Filtered Post 2")
                        .pubDate("2023-01-02")
                        .region(region)
                        .category(category)
                        .build()
        );

        when(postService.findPostsByFilter(eq(region), eq(category))).thenReturn(mockResponseList);

        // When & Then
        mockMvc.perform(get("/api/v1/posts")
                        .param("region", region)
                        .param("category", category.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Filtered Post 1"))
                .andExpect(jsonPath("$[1].region").value(region));
    }

    @Test
    @DisplayName("필터 없이 게시글 목록 조회 성공 (전체 조회)")
    void getFilteredPosts_successWithoutFilters() throws Exception {
        // Given
        List<PostResponseDto> mockResponseList = Arrays.asList(
                PostResponseDto.builder()
                        .id(1L)
                        .title("All Post 1")
                        .pubDate("2023-01-01")
                        .region("서산시")
                        .category(Category.NEWS)
                        .build(),
                PostResponseDto.builder()
                        .id(2L)
                        .title("All Post 2")
                        .pubDate("2023-01-02")
                        .region("태안군")
                        .category(Category.WELFARE_SENIOR)
                        .build()
        );

        when(postService.findPostsByFilter(any(), any())).thenReturn(mockResponseList);

        // When & Then
        mockMvc.perform(get("/api/v1/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("All Post 1"))
                .andExpect(jsonPath("$[1].region").value("태안군"));
    }

    @Test
    @DisplayName("필터 조건에 맞는 게시글이 없을 때 빈 목록 반환")
    void getFilteredPosts_noContent() throws Exception {
        // Given
        String region = "없는지역";
        Category category = Category.UNKNOWN;

        when(postService.findPostsByFilter(eq(region), eq(category))).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/posts")
                        .param("region", region)
                        .param("category", category.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
