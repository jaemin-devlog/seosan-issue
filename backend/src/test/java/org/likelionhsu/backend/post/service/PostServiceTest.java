package org.likelionhsu.backend.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.common.exception.customexception.PostCustomException;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.dto.response.PostDetailResponseDto;
import org.likelionhsu.backend.post.dto.response.PostResponseDto;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private Post testPost;
    private Post anotherTestPost;

    @BeforeEach
    void setUp() {
        testPost = Post.builder()
                .title("Test Post Title")
                .content("Test Post Content")
                .link("http://test.com")
                .pubDate("2023-01-01")
                .region("서산시")
                .category(Category.PUBLIC_INSTITUTION)
                .crawledAt(LocalDateTime.now())
                .build();

        anotherTestPost = Post.builder()
                .title("Another Test Post Title")
                .content("Another Test Post Content")
                .link("http://another.com")
                .pubDate("2023-01-02")
                .region("서산시")
                .category(Category.NEWS)
                .crawledAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("게시글 ID로 게시글 상세 조회 성공")
    void findPostById_success() {
        // Given
        Post postWithId = Post.builder()
                .title(testPost.getTitle())
                .content(testPost.getContent())
                .link(testPost.getLink())
                .pubDate(testPost.getPubDate())
                .region(testPost.getRegion())
                .category(testPost.getCategory())
                .crawledAt(testPost.getCrawledAt())
                .build();
        try {
            java.lang.reflect.Field idField = postWithId.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(postWithId, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        when(postRepository.findById(anyLong())).thenReturn(Optional.of(postWithId));

        // When
        PostDetailResponseDto result = postService.findPostById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(testPost.getTitle());
        assertThat(result.getContent()).isEqualTo(testPost.getContent());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 ID로 조회 시 예외 발생")
    void findPostById_notFound() {
        // Given
        when(postRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        PostCustomException exception = assertThrows(PostCustomException.class, () ->
                postService.findPostById(999L)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("필터 조건에 맞는 게시글 목록 조회 성공")
    void findPostsByFilter_success() {
        // Given
        String region = "서산시";
        Category category = Category.PUBLIC_INSTITUTION;
        List<Post> filteredPosts = Arrays.asList(testPost);

        // Mock PostRepository.findAll(Specification) to return filteredPosts
        when(postRepository.findAll(any(Specification.class))).thenReturn(filteredPosts);

        // When
        List<PostResponseDto> result = postService.findPostsByFilter(region, category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo(testPost.getTitle());
        assertThat(result.get(0).getRegion()).isEqualTo(testPost.getRegion());
        assertThat(result.get(0).getCategory()).isEqualTo(testPost.getCategory());
    }

    @Test
    @DisplayName("필터 조건에 맞는 게시글이 없을 때 빈 목록 반환")
    void findPostsByFilter_noResult() {
        // Given
        String region = "서산시";
        Category category = Category.WELFARE;

        // Mock PostRepository.findAll(Specification) to return an empty list
        when(postRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        // When
        List<PostResponseDto> result = postService.findPostsByFilter(region, category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("게시글 목록 저장 성공")
    void savePosts_success() {
        // Given
        List<Post> postsToSave = Arrays.asList(testPost, anotherTestPost);

        // When
        postService.savePosts(postsToSave);

        // Then
        // Verify that saveAll was called exactly once with the correct list of posts
        verify(postRepository, times(1)).saveAll(postsToSave);
    }
}