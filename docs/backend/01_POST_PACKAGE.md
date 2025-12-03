# 📝 Post 패키지 상세 문서

## 📦 패키지 구조
```
org.likelionhsu.backend.post/
├── controller/
│   └── PostController.java
├── domain/
│   ├── Post.java
│   ├── Category.java
│   └── SourceType.java
├── dto/
│   ├── response/
│   │   ├── PostResponseDto.java
│   │   └── PostDetailResponseDto.java
├── repository/
│   ├── PostRepository.java
│   └── PostSpecification.java
└── service/
    └── PostService.java
```

---

## 🎯 패키지 역할
게시글(Post) 도메인의 핵심 비즈니스 로직을 담당하며, 서산시 관련 공지사항, 복지정보, 문화소식 등의 게시글을 관리합니다.

---

## 📋 클래스별 상세 설명

### 1. Domain Layer

#### 🔷 Post.java
**역할**: 게시글 엔티티 (JPA Entity)

**주요 필드**:
- `id`: 게시글 고유 식별자 (Primary Key)
- `title`: 게시글 제목
- `content`: 게시글 내용 (TEXT 타입)
- `link`: 원본 게시글 링크 URL
- `pubDate`: 게시일 (크롤링한 데이터의 날짜)
- `region`: 지역 정보 (예: "서산시 전체", "대산읍", "인지면")
- `category`: 게시글 카테고리 (Enum)
- `department`: 담당 부서명
- `views`: 조회수
- `crawledAt`: 크롤링된 시간
- `sourceType`: 게시물 소스 타입 (서산시 공지/네이버 뉴스 등)
- `externalId`: 외부 시스템 식별자 (중복 저장 방지용)

**주요 코드**:
```java
@Entity
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;
}
```

**특징**:
- Builder 패턴 사용으로 객체 생성 편의성 제공
- 불변성 보장을 위한 Protected 기본 생성자
- JPA 연관관계 없이 단순한 엔티티 구조

---

#### 🔷 Category.java
**역할**: 게시글 카테고리를 정의하는 Enum (분류 체계)

**정의된 카테고리**:
- `NEWS`: 뉴스
- `NOTICE`: 공지사항
- `PRESS_RELEASE`: 보도자료
- `CULTURE_NEWS`: 문화소식
- `CITY_TOUR`: 시티투어
- `TOUR_GUIDE`: 관광/안내
- `WELFARE_SENIOR`: 복지정보-어르신
- `WELFARE_DISABLED`: 복지정보-장애인
- `WELFARE_WOMEN_FAMILY`: 복지정보-여성가족
- `WELFARE_CHILD_YOUTH`: 복지정보-아동청소년
- `WELFARE_YOUTH`: 복지정보-청년
- `HEALTH_WELLNESS`: 보건/건강
- `BLOG`: 블로그
- `CAFE`: 카페
- `UNKNOWN`: 미분류

**주요 기능**:
```java
// 한글/영문 다양한 입력값을 Category로 변환
public static Category fromValue(String value)

// 변환 실패시 UNKNOWN 반환
public static Category fromValueOrUnknown(String value)

// 내부적으로 정규화 처리
private static String normalize(String s) {
    // 공백, 슬래시, 언더스코어, 하이픈 등 제거
    return s.replaceAll("[-\\s/_\\.]+", "");
}
```

**특징**:
- `ALIASES` Map을 통한 유연한 입력 처리
- "복지정보-어르신", "복지정보어르신", "WELFARE_SENIOR" 모두 동일하게 인식
- 정규화 로직으로 공백/특수문자 무시

---

#### 🔷 SourceType.java
**역할**: 게시글 출처 구분 Enum

**주요 타입**:
- `SEOSAN_CITY`: 서산시청 공식 게시판
- `NAVER_NEWS`: 네이버 뉴스
- `NAVER_BLOG`: 네이버 블로그
- `NAVER_CAFE`: 네이버 카페
- 기타 외부 소스

---

### 2. Repository Layer

#### 🔷 PostRepository.java
**역할**: JPA Repository 인터페이스

**주요 메서드**:
```java
public interface PostRepository extends 
    JpaRepository<Post, Long>, 
    JpaSpecificationExecutor<Post> {
    
    // Spring Data JPA + Specification 조합
    // 동적 쿼리 지원
}
```

**특징**:
- `JpaSpecificationExecutor` 상속으로 동적 쿼리 지원
- 별도 커스텀 메서드 없이 Specification으로 복잡한 검색 처리

---

#### 🔷 PostSpecification.java
**역할**: 동적 쿼리 생성을 위한 JPA Specification 클래스

**주요 메서드**:
```java
public class PostSpecification {
    // 지역으로 필터링
    public static Specification<Post> hasRegion(String region) {
        return (root, query, cb) -> 
            region == null ? null : cb.equal(root.get("region"), region);
    }
    
    // 카테고리로 필터링
    public static Specification<Post> hasCategory(Category category) {
        return (root, query, cb) -> 
            category == null ? null : cb.equal(root.get("category"), category);
    }
}
```

**사용 예시**:
```java
Specification<Post> spec = Specification
    .where(PostSpecification.hasRegion(region))
    .and(PostSpecification.hasCategory(category));
postRepository.findAll(spec, pageable);
```

---

### 3. Service Layer

#### 🔷 PostService.java
**역할**: 게시글 비즈니스 로직 처리

**주요 메서드**:

1. **필터링 조회**:
```java
public Page<PostResponseDto> findPostsByFilter(
    String region, 
    Category category, 
    int page, 
    int size
)
```
- 지역/카테고리 기반 동적 필터링
- 페이징 처리
- DTO 변환 후 반환

2. **상세 조회**:
```java
public PostDetailResponseDto findPostById(Long postId)
```
- 게시글 ID로 단건 조회
- 없을 경우 `PostCustomException` 발생

3. **일괄 저장**:
```java
@Transactional
public void savePosts(List<Post> posts)
```
- 크롤러에서 수집한 게시글 일괄 저장
- 트랜잭션 보장

**특징**:
- `@Transactional(readOnly = true)` 클래스 레벨 적용 (읽기 최적화)
- 쓰기 메서드만 `@Transactional` 오버라이드
- DTO 패턴으로 엔티티 직접 노출 방지

---

### 4. Controller Layer

#### 🔷 PostController.java
**역할**: 게시글 API 엔드포인트 제공

**API 엔드포인트**:

1. **게시글 목록 조회**:
```java
GET /api/posts
Query Parameters:
  - region (optional): 지역 필터
  - category (optional): 카테고리 필터
  - page (default: 0): 페이지 번호
  - size (default: 10): 페이지 크기
  
Response: Page<PostResponseDto>
```

2. **게시글 상세 조회**:
```java
GET /api/posts/{postId}
Path Variable:
  - postId: 게시글 ID
  
Response: PostDetailResponseDto
```

**주요 코드**:
```java
@GetMapping
public ResponseEntity<Page<PostResponseDto>> getFilteredPosts(
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) {
    Category categoryEnum = null;
    if (category != null && !category.isEmpty()) {
        categoryEnum = Category.fromValue(category);
    }
    Page<PostResponseDto> posts = postService.findPostsByFilter(
        region, categoryEnum, page, size
    );
    return ResponseEntity.ok(posts);
}
```

**특징**:
- RESTful API 설계
- 선택적 필터 파라미터 지원
- String → Category Enum 변환 처리
- Spring Data Page 객체로 페이징 정보 포함

---

### 5. DTO Layer

#### 🔷 PostResponseDto.java
**역할**: 게시글 목록 응답용 DTO (간소화된 정보)

**주요 필드**:
- id, title, pubDate, region, category

---

#### 🔷 PostDetailResponseDto.java
**역할**: 게시글 상세 응답용 DTO (전체 정보)

**주요 필드**:
- id, title, content, link, pubDate, region, category
- department, views, crawledAt

**변환 메서드**:
```java
public static PostDetailResponseDto from(Post post) {
    return PostDetailResponseDto.builder()
        .id(post.getId())
        .title(post.getTitle())
        // ...
        .build();
}
```

---

## 🔄 데이터 흐름

```
1. 클라이언트 요청
   ↓
2. PostController (요청 검증, 파라미터 변환)
   ↓
3. PostService (비즈니스 로직)
   ↓
4. PostRepository + PostSpecification (동적 쿼리)
   ↓
5. Database (MySQL)
   ↓
6. Post Entity → DTO 변환
   ↓
7. 클라이언트 응답
```

---

## 💡 주요 설계 포인트

1. **계층 분리**: Controller → Service → Repository 명확한 역할 분담
2. **DTO 패턴**: 엔티티 직접 노출 방지, API 응답 형태 제어
3. **동적 쿼리**: Specification 패턴으로 유연한 검색 조건 조합
4. **Enum 활용**: Category로 타입 안전성 보장, 다양한 입력값 정규화
5. **페이징**: Spring Data의 Page/Pageable 활용
6. **예외 처리**: Custom Exception으로 명확한 에러 핸들링

---

## 🧪 테스트 가능 시나리오

1. 전체 게시글 조회 (필터 없음)
2. 특정 지역("대산읍") 게시글만 조회
3. 특정 카테고리("복지정보-어르신") 게시글만 조회
4. 지역 + 카테고리 복합 필터링
5. 페이징 처리 (2페이지, 20개씩)
6. 존재하지 않는 게시글 조회 시 예외 처리
7. 다양한 카테고리 입력값 정규화 테스트

---

## 📌 연관 패키지

- **User**: 사용자 정보
- **Bookmark**: 게시글 북마크 기능
- **Like**: 게시글 좋아요 기능
- **Comment**: 게시글 댓글 기능
- **Flask Crawler**: 게시글 수집 및 저장

---

## 🔗 참고 API 예시

### 요청 예시
```bash
# 전체 조회
GET /api/posts?page=0&size=10

# 지역 필터
GET /api/posts?region=대산읍

# 카테고리 필터 (한글)
GET /api/posts?category=복지정보-어르신

# 복합 필터
GET /api/posts?region=서산시 전체&category=NOTICE&page=0&size=20

# 상세 조회
GET /api/posts/123
```

### 응답 예시
```json
{
  "content": [
    {
      "id": 123,
      "title": "서산시 여름축제 안내",
      "pubDate": "2025.08.10",
      "region": "서산시 전체",
      "category": "CULTURE_NEWS"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 231,
  "totalPages": 24,
  "last": false
}
```

