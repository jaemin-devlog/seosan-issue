# 👤 User 패키지 상세 문서

## 📦 패키지 구조
```
org.likelionhsu.backend.user/
├── Controller/
│   └── UserController.java
├── Service/
│   ├── UserService.java
│   ├── CustomUserDetailsService.java
│   ├── UserDetailsImpl.java
│   └── MyPageService.java
├── Repository/
│   └── UserRepository.java
├── Enitity/
│   └── User.java
└── Dto/
    ├── SignUpRequest.java
    ├── LoginRequest.java
    ├── RefreshTokenRequest.java
    ├── TokenResponse.java
    ├── UserResponse.java
    ├── MyPagePostDto.java
    └── MyPageCommentDto.java
```

---

## 🎯 패키지 역할
사용자 인증/인가, 회원 관리, 마이페이지 기능을 담당하는 핵심 도메인입니다.

---

## 📋 클래스별 상세 설명

### 1. Entity Layer

#### 🔷 User.java
**역할**: 사용자 엔티티 (JPA Entity)

**주요 필드**:
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(name = "pass_hash", nullable = false, length = 255)
    private String passHash;  // BCrypt 암호화된 비밀번호
    
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime deletedAt;
}
```

**UserStatus Enum**:
- `ACTIVE`: 활성 계정
- `DELETED`: 탈퇴 계정

**주요 메서드**:
```java
public void delete() {
    this.status = UserStatus.DELETED;
    this.deletedAt = LocalDateTime.now();
}
```

**특징**:
- 비밀번호는 평문 저장 X, BCrypt 해시로 저장 (`passHash`)
- Soft Delete 방식 (DB에서 물리 삭제 X, 상태만 변경)
- `@CreationTimestamp`, `@UpdateTimestamp`로 자동 시간 관리
- 이메일, 닉네임 Unique 제약조건

---

### 2. Repository Layer

#### 🔷 UserRepository.java
**역할**: JPA Repository

**주요 메서드**:
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
```

**특징**:
- 이메일로 사용자 조회 (로그인)
- 중복 체크 메서드 제공

---

### 3. Service Layer

#### 🔷 UserService.java
**역할**: 사용자 인증/회원 관리 핵심 비즈니스 로직

**1. 회원가입**:
```java
@Transactional
public UserResponse signUp(SignUpRequest request) {
    // 1. 이메일 중복 체크
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("이미 존재하는 이메일입니다");
    }
    
    // 2. 닉네임 중복 체크
    if (userRepository.existsByNickname(request.getNickname())) {
        throw new IllegalArgumentException("이미 존재하는 닉네임입니다");
    }
    
    // 3. 비밀번호 해시화
    User user = User.builder()
        .email(request.getEmail())
        .passHash(passwordEncoder.encode(request.getPassword()))
        .nickname(request.getNickname())
        .status(User.UserStatus.ACTIVE)
        .build();
    
    User savedUser = userRepository.save(user);
    return UserResponse.from(savedUser);
}
```

**2. 로그인**:
```java
@Transactional(readOnly = true)
public TokenResponse login(LoginRequest request) {
    // 1. 이메일로 사용자 조회
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new IllegalArgumentException(
            "이메일 또는 비밀번호가 올바르지 않습니다"));
    
    // 2. 탈퇴 계정 체크
    if (user.getStatus() == User.UserStatus.DELETED) {
        throw new IllegalArgumentException("탈퇴한 계정입니다");
    }
    
    // 3. 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), user.getPassHash())) {
        throw new IllegalArgumentException(
            "이메일 또는 비밀번호가 올바르지 않습니다");
    }
    
    // 4. JWT 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(
        user.getId(), user.getEmail()
    );
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    
    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .userId(user.getId())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .build();
}
```

**3. 토큰 갱신**:
```java
@Transactional(readOnly = true)
public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();
    
    // 1. 리프레시 토큰 검증
    if (!jwtTokenProvider.validateToken(refreshToken) || 
        !jwtTokenProvider.isRefreshToken(refreshToken)) {
        throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다");
    }
    
    // 2. 사용자 정보 조회
    Long userId = jwtTokenProvider.getUserId(refreshToken);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 3. 탈퇴 계정 체크
    if (user.getStatus() == User.UserStatus.DELETED) {
        throw new IllegalArgumentException("탈퇴한 계정입니다");
    }
    
    // 4. 새 액세스 토큰 발급
    String newAccessToken = jwtTokenProvider.createAccessToken(
        user.getId(), user.getEmail()
    );
    
    return TokenResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshToken)  // 기존 리프레시 토큰 재사용
        .userId(user.getId())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .build();
}
```

**4. 회원 탈퇴**:
```java
@Transactional
public void deleteAccount(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    if (user.getStatus() == User.UserStatus.DELETED) {
        throw new IllegalArgumentException("이미 탈퇴한 계정입니다");
    }
    
    user.delete();  // Soft Delete
    userRepository.save(user);
}
```

**5. 사용자 정보 조회**:
```java
@Transactional(readOnly = true)
public UserResponse getUserInfo(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    return UserResponse.from(user);
}
```

---

#### 🔷 CustomUserDetailsService.java
**역할**: Spring Security용 사용자 정보 로드 서비스

**주요 메서드**:
```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String userId) 
            throws UsernameNotFoundException {
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new UsernameNotFoundException(
                "사용자를 찾을 수 없습니다: " + userId));
        
        return new UserDetailsImpl(user);
    }
}
```

**특징**:
- JWT 토큰에서 추출한 userId로 사용자 조회
- `UserDetailsImpl`로 래핑하여 반환

---

#### 🔷 UserDetailsImpl.java
**역할**: Spring Security UserDetails 구현체

**주요 코드**:
```java
@Getter
public class UserDetailsImpl implements UserDetails {
    private final User user;
    
    public UserDetailsImpl(User user) {
        this.user = user;
    }
    
    public Long getUserId() {
        return user.getId();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getPassword() {
        return user.getPassHash();
    }
    
    @Override
    public String getUsername() {
        return String.valueOf(user.getId());
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() {
        return user.getStatus() == User.UserStatus.ACTIVE;
    }
}
```

**특징**:
- User 엔티티를 감싸는 어댑터 역할
- 모든 사용자에게 `ROLE_USER` 권한 부여
- `isEnabled()`로 탈퇴 계정 비활성화

---

#### 🔷 MyPageService.java
**역할**: 마이페이지 기능 (북마크/좋아요/댓글 목록 조회)

**주요 메서드**:
```java
@Service
@RequiredArgsConstructor
public class MyPageService {
    private final BookmarkRepository bookmarkRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    
    // 내가 북마크한 게시글 목록
    public Page<MyPagePostDto> getBookmarkedPosts(User user, Pageable pageable) {
        return bookmarkRepository
            .findByUserOrderByCreatedAtDesc(user, pageable)
            .map(bookmark -> MyPagePostDto.from(bookmark.getPost()));
    }
    
    // 내가 좋아요한 게시글 목록
    public Page<MyPagePostDto> getLikedPosts(User user, Pageable pageable) {
        return postLikeRepository
            .findByUserOrderByCreatedAtDesc(user, pageable)
            .map(like -> MyPagePostDto.from(like.getPost()));
    }
    
    // 내가 작성한 댓글 목록
    public Page<MyPageCommentDto> getMyComments(User user, Pageable pageable) {
        return commentRepository
            .findByUserOrderByCreatedAtDesc(user, pageable)
            .map(MyPageCommentDto::from);
    }
}
```

---

### 4. Controller Layer

#### 🔷 UserController.java
**역할**: 사용자 관련 API 엔드포인트 제공

**API 엔드포인트**:

**1. 회원가입**:
```java
POST /api/users/signup
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "password123!",
  "nickname": "서산시민"
}

Response:
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "서산시민",
  "status": "ACTIVE"
}
```

**2. 로그인**:
```java
POST /api/users/login
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "password123!"
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "user@example.com",
  "nickname": "서산시민"
}
```

**3. 토큰 갱신**:
```java
POST /api/users/refresh
Content-Type: application/json

Request Body:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // 새로 발급
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // 기존 토큰
  "userId": 1,
  "email": "user@example.com",
  "nickname": "서산시민"
}
```

**4. 회원 탈퇴**:
```java
DELETE /api/users/me
Authorization: Bearer {accessToken}

Response: 204 No Content
```

**5. 내 정보 조회**:
```java
GET /api/users/me
Authorization: Bearer {accessToken}

Response:
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "서산시민",
  "status": "ACTIVE"
}
```

**6. 마이페이지 - 북마크 목록**:
```java
GET /api/users/me/bookmarks?page=0&size=10
Authorization: Bearer {accessToken}

Response: Page<MyPagePostDto>
```

**7. 마이페이지 - 좋아요 목록**:
```java
GET /api/users/me/likes?page=0&size=10
Authorization: Bearer {accessToken}

Response: Page<MyPagePostDto>
```

**8. 마이페이지 - 내 댓글 목록**:
```java
GET /api/users/me/comments?page=0&size=10
Authorization: Bearer {accessToken}

Response: Page<MyPageCommentDto>
```

**인증 처리**:
```java
@DeleteMapping("/me")
public ResponseEntity<Void> deleteAccount(
    @AuthenticationPrincipal UserDetailsImpl userDetails
) {
    userService.deleteAccount(userDetails.getUserId());
    return ResponseEntity.noContent().build();
}
```

**특징**:
- `@AuthenticationPrincipal`로 현재 로그인 사용자 정보 주입
- JWT 필터에서 인증된 사용자만 접근 가능
- `@Valid`로 요청 DTO 검증

---

### 5. DTO Layer

#### 🔷 SignUpRequest.java
**역할**: 회원가입 요청 DTO

**필드**:
- `email`: 이메일 (필수)
- `password`: 비밀번호 (필수)
- `nickname`: 닉네임 (필수)

---

#### 🔷 LoginRequest.java
**역할**: 로그인 요청 DTO

**필드**:
- `email`: 이메일
- `password`: 비밀번호

---

#### 🔷 RefreshTokenRequest.java
**역할**: 토큰 갱신 요청 DTO

**필드**:
- `refreshToken`: 리프레시 토큰

---

#### 🔷 TokenResponse.java
**역할**: 토큰 응답 DTO

**필드**:
- `accessToken`: 액세스 토큰 (15분 유효)
- `refreshToken`: 리프레시 토큰 (7일 유효)
- `userId`: 사용자 ID
- `email`: 이메일
- `nickname`: 닉네임

---

#### 🔷 UserResponse.java
**역할**: 사용자 정보 응답 DTO

**필드**:
- `id`: 사용자 ID
- `email`: 이메일
- `nickname`: 닉네임
- `status`: 계정 상태

---

#### 🔷 MyPagePostDto.java
**역할**: 마이페이지 게시글 DTO (북마크/좋아요 목록용)

---

#### 🔷 MyPageCommentDto.java
**역할**: 마이페이지 댓글 DTO (내 댓글 목록용)

---

## 🔄 인증 흐름

### 회원가입 → 로그인 흐름
```
1. 클라이언트: POST /api/users/signup
   ↓
2. UserController: 요청 검증
   ↓
3. UserService: 중복 체크 + 비밀번호 해시화 + DB 저장
   ↓
4. 클라이언트: 회원가입 성공 응답

5. 클라이언트: POST /api/users/login
   ↓
6. UserController: 로그인 요청
   ↓
7. UserService: 이메일 조회 + 비밀번호 검증
   ↓
8. JwtTokenProvider: JWT 토큰 생성 (Access + Refresh)
   ↓
9. 클라이언트: 토큰 응답
```

### API 요청 인증 흐름
```
1. 클라이언트: GET /api/users/me (Header: Authorization: Bearer {token})
   ↓
2. JwtAuthenticationFilter: 토큰 추출 및 검증
   ↓
3. JwtTokenProvider: 토큰 유효성 확인 + userId 추출
   ↓
4. CustomUserDetailsService: userId로 User 조회
   ↓
5. SecurityContext에 인증 정보 저장
   ↓
6. UserController: @AuthenticationPrincipal로 User 주입
   ↓
7. UserService: 비즈니스 로직 실행
   ↓
8. 클라이언트: 응답
```

### 토큰 갱신 흐름
```
1. 클라이언트: Access Token 만료 감지
   ↓
2. POST /api/users/refresh (Refresh Token 전송)
   ↓
3. UserService: Refresh Token 검증
   ↓
4. JwtTokenProvider: 새 Access Token 발급
   ↓
5. 클라이언트: 새 Access Token 받아 저장
```

---

## 💡 주요 설계 포인트

1. **보안**:
   - 비밀번호 BCrypt 해시 저장
   - JWT 기반 Stateless 인증
   - Refresh Token으로 장기 로그인 지원

2. **Soft Delete**:
   - 물리 삭제 대신 상태 변경
   - 데이터 복구 가능
   - 감사 추적 용이

3. **토큰 분리**:
   - Access Token: 짧은 유효기간 (15분)
   - Refresh Token: 긴 유효기간 (7일)
   - 보안성 ↑, 사용자 편의성 ↑

4. **Spring Security 통합**:
   - UserDetails/UserDetailsService 구현
   - `@AuthenticationPrincipal`로 사용자 정보 주입
   - 선언적 보안 처리

5. **마이페이지 통합**:
   - 북마크/좋아요/댓글을 한 서비스에서 관리
   - 페이징 지원

---

## 🔗 연관 패키지

- **JWT**: 토큰 생성 및 검증
- **SecurityConfig**: Spring Security 설정
- **Bookmark**: 북마크 기능
- **Like**: 좋아요 기능
- **Comment**: 댓글 기능

---

## 📌 보안 고려사항

1. **비밀번호**: 평문 저장 금지, BCrypt 사용
2. **토큰**: HTTPS 통신 필수
3. **중복 가입 방지**: 이메일/닉네임 Unique 제약
4. **탈퇴 계정 접근 차단**: 로그인 시 status 체크
5. **에러 메시지**: 상세 정보 노출 금지 ("이메일 또는 비밀번호가 올바르지 않습니다")

