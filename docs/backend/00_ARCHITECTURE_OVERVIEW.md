# 📚 서산이슈 백엔드 아키텍처 전체 개요

## 🎯 프로젝트 개요

**서산이슈(Seosan-Issue)**는 AI 기반 충남 서산 지역 정보 통합·분석 플랫폼입니다.  
서산시청·읍면동 게시판·문화/관광 공지·뉴스/블로그를 **자동 수집·요약·분류**해 한 곳에서 제공합니다.

---

## 📦 패키지 구조 전체 개요

```
org.likelionhsu.backend/
│
├─ 📰 post/              # 게시글 도메인 (CRUD, 검색, 필터링)
├─ 👤 user/              # 사용자 인증/회원 관리
├─ 🔐 jwt/               # JWT 토큰 생성/검증
├─ 🔖 bookmark/          # 북마크 기능
├─ ❤️ like/              # 좋아요 기능
├─ 💬 comment/           # 댓글 기능
├─ 🤖 ai/                # AI 검색 및 요약
├─ 🔍 naversearch/       # 네이버 API 연동
├─ 🐍 flask/             # Flask 크롤러 연동
├─ ⏰ scheduler/         # 자동 크롤링 스케줄러
├─ ⚙️ common/            # 공통 설정/예외 처리
└─ 🔧 config/            # Spring Security 설정
```

---

## 📋 패키지별 역할 요약

| 패키지 | 역할 | 주요 기능 | 문서 |
|--------|------|-----------|------|
| **Post** | 게시글 도메인 | 게시글 조회, 필터링, 페이징 | [📄 01_POST_PACKAGE.md](./01_POST_PACKAGE.md) |
| **User** | 사용자 관리 | 회원가입, 로그인, 마이페이지 | [📄 02_USER_PACKAGE.md](./02_USER_PACKAGE.md) |
| **JWT** | 인증 토큰 | Access/Refresh Token 생성/검증 | [📄 03_JWT_PACKAGE.md](./03_JWT_PACKAGE.md) |
| **Bookmark/Like/Comment** | 사용자 인터랙션 | 북마크, 좋아요, 댓글 작성/관리 | [📄 04_INTERACTION_PACKAGES.md](./04_INTERACTION_PACKAGES.md) |
| **AI** | AI 검색 | 내부+외부 통합 검색, KoBART 요약 | [📄 05_AI_PACKAGE.md](./05_AI_PACKAGE.md) |
| **NaverSearch** | 외부 검색 | 네이버 뉴스/블로그/트렌드 API | [📄 06_EXTERNAL_INTEGRATION_PACKAGES.md](./06_EXTERNAL_INTEGRATION_PACKAGES.md) |
| **Flask** | 크롤러 연동 | Flask 서버 통신, 요약 모델 호출 | [📄 06_EXTERNAL_INTEGRATION_PACKAGES.md](./06_EXTERNAL_INTEGRATION_PACKAGES.md) |
| **Scheduler** | 자동화 | 3시간마다 자동 크롤링 | [📄 06_EXTERNAL_INTEGRATION_PACKAGES.md](./06_EXTERNAL_INTEGRATION_PACKAGES.md) |
| **Common** | 전역 설정 | 캐싱, CORS, 예외 처리, WebClient | [📄 07_COMMON_CONFIG_PACKAGES.md](./07_COMMON_CONFIG_PACKAGES.md) |
| **Config** | 보안 설정 | Spring Security, JWT 필터 | [📄 07_COMMON_CONFIG_PACKAGES.md](./07_COMMON_CONFIG_PACKAGES.md) |

---

## 🏗 아키텍처 다이어그램

### 전체 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      프론트엔드 (React)                      │
│                     localhost:3000 /                         │
│                     seosan-issue.com                         │
└─────────────┬───────────────────────────────────────────────┘
              │ HTTP/HTTPS (REST API)
              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Backend (8083)                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Controller Layer                                    │   │
│  │  - PostController                                    │   │
│  │  - UserController                                    │   │
│  │  - AiSearchController                                │   │
│  │  - BookmarkController / LikeController / Comment... │   │
│  └────────────┬─────────────────────────────────────────┘   │
│               ↓                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Service Layer                                       │   │
│  │  - PostService                                       │   │
│  │  - UserService                                       │   │
│  │  - SummarizationOrchestrator (AI)                    │   │
│  │  - NaverSearchService                                │   │
│  └────────────┬─────────────────────────────────────────┘   │
│               ↓                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Repository Layer (JPA)                              │   │
│  │  - PostRepository                                    │   │
│  │  - UserRepository                                    │   │
│  │  - BookmarkRepository / LikeRepository / Comment... │   │
│  └────────────┬─────────────────────────────────────────┘   │
└───────────────┼──────────────────────────────────────────────┘
                │                    ↑ ↓
      ┌─────────┴────────┐    ┌──────────────┐
      │                  │    │              │
      ↓                  ↓    │              ↓
┌──────────┐   ┌──────────────┐   ┌─────────────────┐
│  MySQL   │   │ Flask Crawler│   │  Naver API      │
│  (3306)  │   │    (5001)    │   │  (openapi.naver)│
│          │   │              │   │                 │
│ - posts  │   │ - 크롤링     │   │ - 검색 API      │
│ - users  │   │ - 요약(BART) │   │ - 트렌드 API    │
│ - likes  │   │ - DB 저장    │   │                 │
│ - ...    │   │              │   │                 │
└──────────┘   └──────────────┘   └─────────────────┘
```

---

### 계층별 책임

```
┌────────────────────────────────────────────────────┐
│  Controller Layer                                   │
│  - HTTP 요청/응답 처리                              │
│  - DTO 변환                                         │
│  - @AuthenticationPrincipal 사용자 주입             │
│  - 입력 검증 (@Valid)                               │
└─────────────────┬──────────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────────┐
│  Service Layer                                      │
│  - 비즈니스 로직                                    │
│  - 트랜잭션 관리 (@Transactional)                   │
│  - 외부 API 호출 (WebClient)                        │
│  - 권한 검증                                        │
└─────────────────┬──────────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────────┐
│  Repository Layer                                   │
│  - 데이터 접근 (JPA)                                │
│  - 동적 쿼리 (Specification)                        │
│  - 페이징 (Pageable)                                │
└─────────────────┬──────────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────────┐
│  Database (MySQL)                                   │
│  - 영속성 계층                                      │
└────────────────────────────────────────────────────┘
```

---

## 🔄 주요 데이터 흐름

### 1. 사용자 인증 흐름

```
[회원가입]
POST /api/users/signup
  ↓
UserController
  ↓
UserService
  - 이메일 중복 체크
  - 비밀번호 BCrypt 암호화
  - DB 저장
  ↓
Response: UserResponse

[로그인]
POST /api/users/login
  ↓
UserController
  ↓
UserService
  - 이메일로 사용자 조회
  - 비밀번호 검증 (BCrypt.matches)
  ↓
JwtTokenProvider
  - Access Token 생성 (15분)
  - Refresh Token 생성 (7일)
  ↓
Response: TokenResponse (tokens + user info)

[API 요청 (인증 필요)]
GET /api/users/me
Authorization: Bearer {accessToken}
  ↓
JwtAuthenticationFilter
  - 토큰 추출
  - 검증 (만료, 서명)
  - userId 추출
  ↓
CustomUserDetailsService
  - loadUserByUsername(userId)
  - User 조회
  ↓
SecurityContext에 인증 정보 저장
  ↓
UserController
  - @AuthenticationPrincipal UserDetailsImpl userDetails
  ↓
Response: UserResponse
```

---

### 2. 게시글 조회 흐름

```
[게시글 목록 조회]
GET /api/posts?region=대산읍&category=복지정보-어르신&page=0&size=10
  ↓
PostController
  - region, category 파라미터 파싱
  - Category.fromValue("복지정보-어르신") → Enum 변환
  ↓
PostService
  ↓
PostRepository
  - Specification.where(hasRegion).and(hasCategory)
  - Pageable로 페이징 처리
  ↓
Query:
  SELECT * FROM post
  WHERE region = '대산읍'
    AND category = 'WELFARE_SENIOR'
  ORDER BY pub_date DESC
  LIMIT 10 OFFSET 0
  ↓
Post 엔티티 → PostResponseDto 변환
  ↓
Response: Page<PostResponseDto>
```

---

### 3. AI 검색 흐름

```
[AI 검색 요청]
POST /api/v1/ai-search
{
  "query": "서산 축제",
  "maxExternal": 3
}
  ↓
AiSearchController
  ↓
SummarizationOrchestrator.summarize()

[1단계: 내부 검색]
  ↓
PostRepository
  - findAll(PostSpecification.containsKeyword("서산 축제"))
  - 결과: [Post1, Post2, ...]

[2단계: 외부 검색]
  ↓
NaverSearchService.search("news", "서산 축제", 3)
  ↓
WebClient → https://openapi.naver.com/v1/search/news.json
  - 결과: [NaverItem1, NaverItem2, ...]

[3단계: 본문 크롤링]
  ↓
ContentFetcher.fetch(url)
  - Jsoup으로 HTML 파싱
  - article, .article, #content 셀렉터
  - 결과: ArticleText(title, body)

[4단계: Map (개별 요약)]
  ↓
PerDocSummarizer.summarizeOne()
  ↓
FlaskSummarizeClient.summarize(system, user)
  ↓
WebClient → http://crawler:5001/summarize
  - 요청: { "system": "...", "user": "..." }
  - Flask: KoBART 모델 로드 → 토크나이징 → generate()
  - 응답: { "summary": "1-2문장 요약" }
  ↓
결과: [PerDocSummary1, PerDocSummary2, ...]

[5단계: Reduce (통합 요약)]
  ↓
FlaskSummarizeClient.summarize(reduceSystem, reduceUser)
  - 모든 개별 요약 통합
  - Flask: KoBART 최종 요약 → "3-5줄 TL;DR"
  ↓
후처리
  - sanitizeResponse() (불필요한 토큰 제거)
  - postClean() (공백 정리)
  ↓
Response:
{
  "summary": "서산시는 8월 10-15일 여름축제 개최...",
  "sources": ["url1", "url2", ...]
}
```

---

### 4. 자동 크롤링 흐름

```
[3시간마다]
  ↓
CrawlScheduler.scheduleCrawlAll()
  ↓
FlaskController.crawlAll(2)
  ↓
FlaskService.triggerCrawlAll(2)
  ↓
WebClient → http://crawler:5001/crawl_all?pages=2
  ↓
Flask 크롤러 (Python)
  - 서산시청 게시판 크롤링
  - 읍면동 게시판 크롤링
  - 문화관광 게시판 크롤링
  ↓
각 게시글마다
  - KoBART 요약 (1-2문장)
  - MySQL 저장 (INSERT INTO post ...)
  ↓
Response: "크롤링 완료 (XX건 수집)"
  ↓
로그 출력
  ↓
[다음 3시간 대기]
```

---

## 🔑 핵심 기술 스택

| 영역 | 기술 | 역할 |
|------|------|------|
| **Framework** | Spring Boot 3.x | 백엔드 프레임워크 |
| **Language** | Java 21 | 프로그래밍 언어 |
| **Database** | MySQL 8.0 | RDBMS |
| **ORM** | Spring Data JPA | 데이터 접근 계층 |
| **Security** | Spring Security + JWT | 인증/인가 |
| **Cache** | Caffeine (+ Redis 옵션) | 인메모리 캐싱 |
| **HTTP Client** | WebClient (Reactor) | 비동기 HTTP 통신 |
| **Validation** | Hibernate Validator | 입력 검증 |
| **Build Tool** | Gradle 8.x | 빌드/의존성 관리 |
| **AI Model** | KoBART (Flask) | 텍스트 요약 |
| **External API** | Naver Search API | 외부 콘텐츠 검색 |
| **Crawler** | Python Flask + Jsoup | 웹 크롤링 |
| **Containerization** | Docker + Docker Compose | 컨테이너 오케스트레이션 |

---

## 📊 데이터베이스 스키마

### 주요 테이블

```
┌─────────────────────────────────────────────────────┐
│  users                                               │
│  ┌──────────────┬─────────────────────────────────┐ │
│  │ id           │ BIGINT PK AUTO_INCREMENT         │ │
│  │ email        │ VARCHAR(100) UNIQUE              │ │
│  │ pass_hash    │ VARCHAR(255)                     │ │
│  │ nickname     │ VARCHAR(50) UNIQUE               │ │
│  │ status       │ ENUM('ACTIVE', 'DELETED')        │ │
│  │ created_at   │ DATETIME                         │ │
│  │ updated_at   │ DATETIME                         │ │
│  │ deleted_at   │ DATETIME                         │ │
│  └──────────────┴─────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  post                                                │
│  ┌──────────────┬─────────────────────────────────┐ │
│  │ id           │ BIGINT PK AUTO_INCREMENT         │ │
│  │ title        │ VARCHAR(255)                     │ │
│  │ content      │ TEXT                             │ │
│  │ link         │ VARCHAR(500)                     │ │
│  │ pub_date     │ VARCHAR(50)                      │ │
│  │ region       │ VARCHAR(50)                      │ │
│  │ category     │ ENUM(...)                        │ │
│  │ department   │ VARCHAR(100)                     │ │
│  │ views        │ INT                              │ │
│  │ crawled_at   │ DATETIME                         │ │
│  │ source_type  │ ENUM(...)                        │ │
│  │ external_id  │ VARCHAR(500)                     │ │
│  └──────────────┴─────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  bookmark                                            │
│  ┌──────────────┬─────────────────────────────────┐ │
│  │ id           │ BIGINT PK AUTO_INCREMENT         │ │
│  │ user_id      │ BIGINT FK → users.id             │ │
│  │ post_id      │ BIGINT FK → post.id              │ │
│  │ created_at   │ DATETIME                         │ │
│  │ UNIQUE(user_id, post_id)                        │ │
│  └──────────────┴─────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  post_like                                           │
│  ┌──────────────┬─────────────────────────────────┐ │
│  │ id           │ BIGINT PK AUTO_INCREMENT         │ │
│  │ user_id      │ BIGINT FK → users.id             │ │
│  │ post_id      │ BIGINT FK → post.id              │ │
│  │ created_at   │ DATETIME                         │ │
│  │ UNIQUE(user_id, post_id)                        │ │
│  └──────────────┴─────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  comment                                             │
│  ┌──────────────┬─────────────────────────────────┐ │
│  │ id           │ BIGINT PK AUTO_INCREMENT         │ │
│  │ post_id      │ BIGINT FK → post.id              │ │
│  │ user_id      │ BIGINT FK → users.id             │ │
│  │ content      │ TEXT                             │ │
│  │ created_at   │ DATETIME                         │ │
│  │ updated_at   │ DATETIME                         │ │
│  └──────────────┴─────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 🔐 보안 고려사항

1. **비밀번호 암호화**: BCrypt 사용 (단방향 해시)
2. **JWT 토큰**: 서명 검증, 만료 시간 체크
3. **HTTPS**: 프로덕션 환경 필수
4. **CORS**: 허용된 도메인만 접근 가능
5. **SQL Injection**: JPA Parameterized Query 사용
6. **XSS**: 입력 검증, HTML 이스케이핑
7. **CSRF**: JWT 사용으로 CSRF 공격 방어
8. **Rate Limiting**: 추후 도입 (API Gateway)

---

## 📈 성능 최적화

1. **캐싱**: Caffeine 인메모리 캐시 (네이버 검색 결과 10분)
2. **Lazy Loading**: @ManyToOne(fetch = LAZY)
3. **페이징**: 대량 데이터 조회 시 Page/Pageable
4. **비동기 처리**: WebClient (Reactor)
5. **인덱스**: region, category, pub_date 컬럼
6. **Connection Pool**: HikariCP (기본 10개)

---

## 🧪 테스트 전략

1. **단위 테스트**: Service/Repository 계층
2. **통합 테스트**: Controller → Service → Repository
3. **API 테스트**: Postman Collection 제공
4. **보안 테스트**: 인증/권한 검증
5. **성능 테스트**: JMeter (부하 테스트)

---

## 📁 문서 인덱스

| 번호 | 문서명 | 내용 |
|------|--------|------|
| 00 | [README.md](./00_ARCHITECTURE_OVERVIEW.md) | **전체 개요 (현재 문서)** |
| 01 | [POST_PACKAGE.md](./01_POST_PACKAGE.md) | 게시글 도메인 상세 |
| 02 | [USER_PACKAGE.md](./02_USER_PACKAGE.md) | 사용자 인증/회원 관리 |
| 03 | [JWT_PACKAGE.md](./03_JWT_PACKAGE.md) | JWT 토큰 시스템 |
| 04 | [INTERACTION_PACKAGES.md](./04_INTERACTION_PACKAGES.md) | 북마크/좋아요/댓글 |
| 05 | [AI_PACKAGE.md](./05_AI_PACKAGE.md) | AI 검색 및 요약 |
| 06 | [EXTERNAL_INTEGRATION_PACKAGES.md](./06_EXTERNAL_INTEGRATION_PACKAGES.md) | 네이버/Flask/Scheduler |
| 07 | [COMMON_CONFIG_PACKAGES.md](./07_COMMON_CONFIG_PACKAGES.md) | 공통 설정/예외 처리 |

---

## 🚀 빠른 시작

### Docker Compose로 실행

```bash
# 1. 환경변수 설정
cp env.example .env
# .env 파일 수정 (DB_PASSWORD, NAVER_CLIENT_ID 등)

# 2. 빌드 및 실행
docker compose up -d --build

# 3. 로그 확인
docker compose logs -f backend

# 4. 접속 확인
curl http://localhost:8083/api/posts
```

### 로컬 개발 모드

```bash
# 1. MySQL 실행 (Docker 또는 로컬)
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=seosan_issue_db \
  mysql:8.0

# 2. 환경변수 설정
export DB_HOST=localhost
export DB_USER=root
export DB_PASSWORD=password
export DB_NAME=seosan_issue_db
export JWT_SECRET=your_jwt_secret_minimum_32_characters
export NAVER_CLIENT_ID=your_client_id
export NAVER_CLIENT_SECRET=your_client_secret

# 3. 백엔드 실행
cd backend
./gradlew bootRun

# 4. 접속 확인
curl http://localhost:8083/api/posts
```

---

## 📞 문의 및 기여

프로젝트 관련 문의나 기여는 GitHub Issues 또는 Pull Request를 이용해 주세요.

---

**작성일**: 2025-12-03  
**버전**: 1.0  
**작성자**: 서산이슈 개발팀

