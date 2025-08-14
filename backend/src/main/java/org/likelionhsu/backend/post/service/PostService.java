package org.likelionhsu.backend.post.service;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.dto.response.PostDetailResponseDto;
import org.likelionhsu.backend.post.dto.response.PostResponseDto;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.stream.Collectors;

import org.likelionhsu.backend.common.exception.customexception.PostCustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;
import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.repository.PostSpecification;
import org.springframework.data.jpa.domain.Specification;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    /**
     * 필터 조건에 맞는 게시글 목록을 조회합니다.
     * @param region 지역 (읍면동)
     * @param category 카테고리
     * @return 필터링된 게시글 목록
     */
    public Page<PostResponseDto> findPostsByFilter(String region, Category category, int page, int size) {
        Specification<Post> spec = Specification.where(PostSpecification.hasRegion(region))
                                                .and(PostSpecification.hasCategory(category));
        
        Pageable pageable = PageRequest.of(page, size);

        return postRepository.findAll(spec, pageable)
                .map(PostResponseDto::from);
    }

    /**
     * 특정 ID의 게시글을 상세 조회합니다.
     * @param postId 조회할 게시글의 ID
     * @return 게시글 상세 정보 (PostDetailResponseDto)
     */
    public PostDetailResponseDto findPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostCustomException(ErrorCode.POST_NOT_FOUND));
        return PostDetailResponseDto.from(post);
    }

    /**
     * 게시글 목록을 저장합니다.
     * @param posts 저장할 게시글 목록
     */
    @Transactional
    public void savePosts(List<Post> posts) {
        postRepository.saveAll(posts);
    }
}
