package org.likelionhsu.backend.bookmark.service;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.bookmark.domain.Bookmark;
import org.likelionhsu.backend.bookmark.repository.BookmarkRepository;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.post.repository.PostRepository;
import org.likelionhsu.backend.user.Enitity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;

    @Transactional
    public void addBookmark(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!bookmarkRepository.existsByUserAndPost(user, post)) {
            Bookmark bookmark = new Bookmark(post, user);
            bookmarkRepository.save(bookmark);
        }
    }

    @Transactional
    public void removeBookmark(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        bookmarkRepository.findByUserAndPost(user, post)
                .ifPresent(bookmarkRepository::delete);
    }
}

