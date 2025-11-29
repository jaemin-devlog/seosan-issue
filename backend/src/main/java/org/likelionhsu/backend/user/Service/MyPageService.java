package org.likelionhsu.backend.user.Service;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.bookmark.repository.BookmarkRepository;
import org.likelionhsu.backend.comment.repository.CommentRepository;
import org.likelionhsu.backend.like.repository.PostLikeRepository;
import org.likelionhsu.backend.post.domain.Post;
import org.likelionhsu.backend.user.Dto.MyPageCommentDto;
import org.likelionhsu.backend.user.Dto.MyPagePostDto;
import org.likelionhsu.backend.user.Enitity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final BookmarkRepository bookmarkRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;

    public Page<MyPagePostDto> getBookmarkedPosts(User user, Pageable pageable) {
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(bookmark -> {
                    Post post = bookmark.getPost();
                    return MyPagePostDto.builder()
                            .postId(post.getId())
                            .title(post.getTitle())
                            .region(post.getRegion())
                            .category(post.getCategory().name())
                            .createdAt(post.getCrawledAt())
                            .viewCount(post.getViews())
                            .likeCount((int) postLikeRepository.countByPost(post))
                            .commentCount((int) commentRepository.findByPostOrderByCreatedAtAsc(post).size())
                            .interactionAt(bookmark.getCreatedAt())
                            .build();
                });
    }

    public Page<MyPagePostDto> getLikedPosts(User user, Pageable pageable) {
        return postLikeRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(postLike -> {
                    Post post = postLike.getPost();
                    return MyPagePostDto.builder()
                            .postId(post.getId())
                            .title(post.getTitle())
                            .region(post.getRegion())
                            .category(post.getCategory().name())
                            .createdAt(post.getCrawledAt())
                            .viewCount(post.getViews())
                            .likeCount((int) postLikeRepository.countByPost(post))
                            .commentCount((int) commentRepository.findByPostOrderByCreatedAtAsc(post).size())
                            .interactionAt(postLike.getCreatedAt())
                            .build();
                });
    }

    public Page<MyPageCommentDto> getMyComments(User user, Pageable pageable) {
        return commentRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(comment -> MyPageCommentDto.builder()
                        .commentId(comment.getId())
                        .postId(comment.getPost().getId())
                        .postTitle(comment.getPost().getTitle())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .build());
    }
}

