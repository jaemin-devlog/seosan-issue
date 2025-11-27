package org.likelionhsu.backend.like.repository;

import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.like.domain.PostLike;
import org.likelionhsu.backend.user.Enitity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserAndPost(User user, Post post);

    Optional<PostLike> findByUserAndPost(User user, Post post);

    long countByPost(Post post);
}