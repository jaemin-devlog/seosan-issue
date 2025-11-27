package org.likelionhsu.backend.like.service;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.like.domain.PostLike;
import org.likelionhsu.backend.like.repository.PostLikeRepository;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.likelionhsu.backend.user.Enitity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    @Transactional
    public void likePost(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!postLikeRepository.existsByUserAndPost(user, post)) {
            PostLike like = new PostLike(post, user);
            postLikeRepository.save(like);
        }
    }

    @Transactional
    public void unlikePost(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        postLikeRepository.findByUserAndPost(user, post)
                .ifPresent(postLikeRepository::delete);
    }
}

