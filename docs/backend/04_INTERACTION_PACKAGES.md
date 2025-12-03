# 🔖 Bookmark, ❤️ Like, 💬 Comment 패키지 상세 문서

이 세 패키지는 사용자 인터랙션 기능을 담당하며, 구조와 로직이 유사하므로 하나의 문서로 통합합니다.

---

## 📦 패키지 구조

### Bookmark 패키지
```
org.likelionhsu.backend.bookmark/
├── controller/
│   └── BookmarkController.java
├── domain/
│   └── Bookmark.java
├── repository/
│   └── BookmarkRepository.java
└── service/
    └── BookmarkService.java
```

### Like 패키지
```
org.likelionhsu.backend.like/
├── controller/
│   └── PostLikeController.java
├── domain/
│   └── PostLike.java
├── repository/
│   └── PostLikeRepository.java
└── service/
    └── PostLikeService.java
```

### Comment 패키지
```
org.likelionhsu.backend.comment/
├── controller/
│   └── CommentController.java
├── domain/
│   └── Comment.java
├── dto/
│   ├── CommentRequestDto.java
│   └── CommentResponseDto.java
├── repository/
│   └── CommentRepository.java
└── service/
    └── CommentService.java
```

---

## 🎯 패키지별 역할

| 패키지 | 역할 | 주요 기능 |
|--------|------|-----------|
| **Bookmark** | 게시글 북마크 | 추가, 삭제, 목록 조회 |
| **Like** | 게시글 좋아요 | 좋아요, 취소 |
| **Comment** | 댓글 관리 | 작성, 조회, 수정, 삭제 |

---

## 📋 1. Bookmark 패키지

### 🔷 Bookmark.java (Entity)
**역할**: 사용자-게시글 북마크 관계 엔티티

```java
@Entity
@Table(name = "bookmark", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public Bookmark(Post post, User user) {
        this.post = post;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }
}
```

**특징**:
- **중복 방지**: `(user_id, post_id)` Unique 제약조건
- **Lazy Loading**: Post, User는 필요할 때만 조회
- **자동 시간 기록**: 북마크 생성 시간 자동 저장

---

### 🔷 BookmarkRepository.java
**역할**: 북마크 데이터 접근 계층

```java
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    // 중복 북마크 체크
    boolean existsByUserAndPost(User user, Post post);
    
    // 특정 북마크 조회
    Optional<Bookmark> findByUserAndPost(User user, Post post);
    
    // 사용자의 북마크 목록 (마이페이지용)
    Page<Bookmark> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
```

---

### 🔷 BookmarkService.java
**역할**: 북마크 비즈니스 로직

**1. 북마크 추가**:
```java
@Transactional
public void addBookmark(Long postId, User user) {
    // 1. 게시글 존재 확인
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    
    // 2. 중복 체크 (이미 북마크한 경우 무시)
    if (!bookmarkRepository.existsByUserAndPost(user, post)) {
        Bookmark bookmark = new Bookmark(post, user);
        bookmarkRepository.save(bookmark);
    }
}
```

**2. 북마크 삭제**:
```java
@Transactional
public void removeBookmark(Long postId, User user) {
    // 1. 게시글 존재 확인
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    
    // 2. 북마크 찾아서 삭제
    bookmarkRepository.findByUserAndPost(user, post)
        .ifPresent(bookmarkRepository::delete);
}
```

**특징**:
- **멱등성**: 중복 추가/삭제 시 에러 없이 무시
- **트랜잭션**: 데이터 일관성 보장

---

### 🔷 BookmarkController.java
**역할**: 북마크 API 엔드포인트

```java
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;
    
    // 북마크 추가
    @PostMapping("/{postId}")
    public ResponseEntity<Void> addBookmark(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        bookmarkService.addBookmark(postId, userDetails.getUser());
        return ResponseEntity.ok().build();
    }
    
    // 북마크 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> removeBookmark(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        bookmarkService.removeBookmark(postId, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
```

**API 예시**:
```bash
# 북마크 추가
POST /api/bookmarks/123
Authorization: Bearer {accessToken}

# 북마크 삭제
DELETE /api/bookmarks/123
Authorization: Bearer {accessToken}

# 내 북마크 목록 (마이페이지)
GET /api/users/me/bookmarks?page=0&size=10
Authorization: Bearer {accessToken}
```

---

## 📋 2. Like 패키지

### 🔷 PostLike.java (Entity)
**역할**: 사용자-게시글 좋아요 관계 엔티티

```java
@Entity
@Table(name = "post_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public PostLike(Post post, User user) {
        this.post = post;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }
}
```

**특징**: Bookmark와 구조 동일 (테이블명만 다름)

---

### 🔷 PostLikeRepository.java
```java
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserAndPost(User user, Post post);
    Optional<PostLike> findByUserAndPost(User user, Post post);
    Page<PostLike> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
```

---

### 🔷 PostLikeService.java
**역할**: 좋아요 비즈니스 로직

```java
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
```

---

### 🔷 PostLikeController.java
```java
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class PostLikeController {
    private final PostLikeService postLikeService;
    
    @PostMapping("/{postId}")
    public ResponseEntity<Void> likePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        postLikeService.likePost(postId, userDetails.getUser());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> unlikePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        postLikeService.unlikePost(postId, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
```

**API 예시**:
```bash
# 좋아요
POST /api/likes/123
Authorization: Bearer {accessToken}

# 좋아요 취소
DELETE /api/likes/123
Authorization: Bearer {accessToken}

# 내가 좋아요한 게시글 목록
GET /api/users/me/likes?page=0&size=10
Authorization: Bearer {accessToken}
```

---

## 📋 3. Comment 패키지

### 🔷 Comment.java (Entity)
**역할**: 댓글 엔티티

```java
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Comment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
    
    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
```

**특징**:
- **수정 가능**: `updateContent()` 메서드 제공
- **수정 시간 기록**: `updatedAt` 필드

---

### 🔷 CommentRepository.java
```java
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 댓글 목록 (생성일 오름차순)
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);
    
    // 사용자의 댓글 목록 (마이페이지용)
    Page<Comment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
```

---

### 🔷 CommentService.java
**역할**: 댓글 비즈니스 로직

**1. 댓글 작성**:
```java
@Transactional
public CommentResponseDto createComment(
    Long postId, 
    CommentRequestDto requestDto, 
    User user
) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    
    Comment comment = new Comment(post, user, requestDto.getContent());
    Comment saved = commentRepository.save(comment);
    return CommentResponseDto.from(saved);
}
```

**2. 댓글 조회**:
```java
@Transactional(readOnly = true)
public List<CommentResponseDto> getComments(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    
    return commentRepository.findByPostOrderByCreatedAtAsc(post).stream()
        .map(CommentResponseDto::from)
        .collect(Collectors.toList());
}
```

**3. 댓글 수정**:
```java
@Transactional
public CommentResponseDto updateComment(
    Long commentId, 
    CommentRequestDto requestDto, 
    User user
) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    
    // 권한 검증: 본인만 수정 가능
    if (!comment.getUser().getId().equals(user.getId())) {
        throw new IllegalArgumentException("본인이 작성한 댓글만 수정할 수 있습니다.");
    }
    
    comment.updateContent(requestDto.getContent());
    return CommentResponseDto.from(comment);
}
```

**4. 댓글 삭제**:
```java
@Transactional
public void deleteComment(Long commentId, User user) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    
    // 권한 검증: 본인만 삭제 가능
    if (!comment.getUser().getId().equals(user.getId())) {
        throw new IllegalArgumentException("본인이 작성한 댓글만 삭제할 수 있습니다.");
    }
    
    commentRepository.delete(comment);
}
```

---

### 🔷 CommentController.java
```java
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    
    // 댓글 작성
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
        @PathVariable Long postId,
        @Valid @RequestBody CommentRequestDto requestDto,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CommentResponseDto response = commentService.createComment(
            postId, requestDto, userDetails.getUser()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(
        @PathVariable Long postId
    ) {
        List<CommentResponseDto> comments = commentService.getComments(postId);
        return ResponseEntity.ok(comments);
    }
    
    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @Valid @RequestBody CommentRequestDto requestDto,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CommentResponseDto response = commentService.updateComment(
            commentId, requestDto, userDetails.getUser()
        );
        return ResponseEntity.ok(response);
    }
    
    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        commentService.deleteComment(commentId, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
```

**API 예시**:
```bash
# 댓글 작성
POST /api/posts/123/comments
Authorization: Bearer {accessToken}
Content-Type: application/json
{
  "content": "좋은 정보 감사합니다!"
}

# 댓글 목록 조회 (인증 불필요)
GET /api/posts/123/comments

# 댓글 수정
PUT /api/posts/123/comments/456
Authorization: Bearer {accessToken}
Content-Type: application/json
{
  "content": "수정된 댓글 내용"
}

# 댓글 삭제
DELETE /api/posts/123/comments/456
Authorization: Bearer {accessToken}

# 내가 작성한 댓글 목록
GET /api/users/me/comments?page=0&size=10
Authorization: Bearer {accessToken}
```

---

### 🔷 DTO

**CommentRequestDto.java**:
```java
@Data
public class CommentRequestDto {
    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;
}
```

**CommentResponseDto.java**:
```java
@Data
@Builder
public class CommentResponseDto {
    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
            .id(comment.getId())
            .postId(comment.getPost().getId())
            .userId(comment.getUser().getId())
            .nickname(comment.getUser().getNickname())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
```

---

## 🔄 공통 데이터 흐름

### Bookmark/Like 흐름
```
1. 클라이언트: POST /api/bookmarks/123 (accessToken 포함)
   ↓
2. JwtAuthenticationFilter: 토큰 검증 → User 인증
   ↓
3. Controller: @AuthenticationPrincipal로 User 주입
   ↓
4. Service: 
   - 게시글 존재 확인
   - 중복 체크
   - Bookmark/Like 엔티티 생성 및 저장
   ↓
5. 클라이언트: 200 OK
```

### Comment 작성 흐름
```
1. 클라이언트: POST /api/posts/123/comments (content + accessToken)
   ↓
2. JwtAuthenticationFilter: 인증 처리
   ↓
3. CommentController: @Valid로 요청 검증
   ↓
4. CommentService:
   - 게시글 존재 확인
   - Comment 엔티티 생성 및 저장
   - DTO 변환
   ↓
5. 클라이언트: 201 Created + CommentResponseDto
```

### Comment 수정/삭제 흐름
```
1. 클라이언트: PUT/DELETE /api/posts/123/comments/456
   ↓
2. JwtAuthenticationFilter: 인증 처리
   ↓
3. CommentService:
   - 댓글 존재 확인
   - 권한 검증 (작성자 == 요청자?)
   - 수정/삭제 실행
   ↓
4. 클라이언트: 200 OK / 204 No Content
```

---

## 💡 주요 설계 포인트

### 1. 멱등성 (Idempotency)
- 북마크/좋아요 중복 추가 시 에러 없이 무시
- 삭제 시 존재하지 않아도 성공 응답
- 안정적인 클라이언트 경험

### 2. 권한 검증
- 댓글 수정/삭제는 본인만 가능
- Service 계층에서 검증 (Controller가 아님)
- 명확한 에러 메시지

### 3. Lazy Loading
- `@ManyToOne(fetch = FetchType.LAZY)`
- N+1 문제 방지
- 필요할 때만 조회

### 4. Unique 제약조건
- `(user_id, post_id)` 복합 Unique
- DB 레벨에서 중복 방지
- 동시성 제어

### 5. DTO 패턴
- 엔티티 직접 노출 방지
- API 응답 형태 제어
- 순환 참조 방지

---

## 🔗 연관 관계

```
User ──(1:N)──> Bookmark ──(N:1)──> Post
User ──(1:N)──> PostLike ──(N:1)──> Post
User ──(1:N)──> Comment  ──(N:1)──> Post
```

---

## 🧪 테스트 시나리오

### Bookmark/Like 공통
1. 북마크/좋아요 추가 → 성공
2. 동일 게시글 재추가 → 멱등성 (중복 저장 X)
3. 북마크/좋아요 삭제 → 성공
4. 존재하지 않는 게시글 → 404
5. 인증 없이 요청 → 401

### Comment
1. 댓글 작성 → 성공 (201)
2. 댓글 목록 조회 → 시간순 정렬
3. 댓글 수정 (본인) → 성공
4. 댓글 수정 (타인) → 403
5. 댓글 삭제 (본인) → 성공
6. 댓글 삭제 (타인) → 403
7. 빈 내용 댓글 → 400 (@Valid 검증)

---

## 📌 향후 개선 방향

1. **좋아요 카운트 캐싱**: Redis로 실시간 카운트
2. **대댓글 기능**: 계층형 댓글 구조
3. **신고 기능**: 부적절한 댓글 신고
4. **알림 기능**: 내 게시글에 댓글 작성 시 알림
5. **소프트 삭제**: 댓글 삭제 시 DB에서 바로 삭제 X

