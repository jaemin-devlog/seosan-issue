package org.likelionhsu.backend.comment.repository;

import org.likelionhsu.backend.comment.domain.Comment;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.user.Enitity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    List<Comment> findByUser(User user);
}

