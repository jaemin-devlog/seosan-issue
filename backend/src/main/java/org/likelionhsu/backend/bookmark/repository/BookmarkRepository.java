package org.likelionhsu.backend.bookmark.repository;

import org.likelionhsu.backend.bookmark.domain.Bookmark;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.user.Enitity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserAndPost(User user, Post post);

    Optional<Bookmark> findByUserAndPost(User user, Post post);

    long countByPost(Post post);
}

