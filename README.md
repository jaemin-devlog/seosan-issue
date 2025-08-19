# 📰 서산이슈 (Seosan-Issue)

> **AI 기반 충남 서산 지역 정보 통합·분석 플랫폼** — 서산시청·읍면동 게시판·문화/관광 공지·뉴스/블로그를 **자동 수집·요약·분류**해 한 곳에서 제공합니다.

---

## ⚡️ TL;DR (빠른 실행)

### 1) Docker Compose로 원커맨드 실행

```bash
# 레포 루트에서
cp env.example .env
# .env 파일 내용 채우기 (예:)
# DB_PASSWORD=변경필수
# DB_NAME=seosan_issue_db
# DB_USER=root
# DB_HOST=db
# NAVER_CLIENT_ID=...
# NAVER_CLIENT_SECRET=...
# KMA_SERVICE_KEY=...

# 빌드 & 기동
docker compose up -d --build
```

* **백엔드**: [http://localhost:8083](http://localhost:8083)
* **Swagger UI**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
* **크롤러(Flask)**: [http://localhost:5001](http://localhost:5001)
* **MySQL**: localhost:3307 (컨테이너 내부 3306)

### 2) 로컬 개발 (개별 실행)

* MySQL 준비 후 스키마 `seosan_issue_db` 생성
* **백엔드**

  ```bash
  cd backend
  ./gradlew bootRun
  ```

  `backend/src/main/resources/application.yml` 기준 환경변수 예시:

  ```properties
  DB_HOST=localhost
  DB_USER=root
  DB_PASSWORD=...
  DB_NAME=seosan_issue_db
  KMA_SERVICE_KEY=...
  NAVER_CLIENT_ID=...
  NAVER_CLIENT_SECRET=...
  ```
* **크롤러(Flask)**

  ```bash
  cd crawler
  pip install -r requirements.txt
  # 환경변수 (DB 접속 및 모델) — .env 사용 가능
  export DB_HOST=localhost
  export DB_USER=root
  export DB_PASSWORD=...
  export DB_NAME=seosan_issue_db
  export PORT=5001
  python api.py
  ```

---

## 🧭 프로젝트 개요

* **문제**: 시정·복지·문화·관광 정보가 여러 채널에 흩어져 **접근성 격차** 발생
* **해결**: 정기 크롤링 → 정제 → **BART 요약** → 카테고리 분류 → 검색/탐색 API 제공
* **핵심 가치**: 하루 단위로 변하는 생활정보를 **실시간에 가깝게 통합**하고, 누구나 이해하기 쉬운 **요약/탐색 인터페이스** 제공

---

## 🏗 아키텍처

```
┌──────────┐     HTTP      ┌───────────────┐
│  프론트  │  <──────────> │   백엔드 API   │ Spring Boot (8083)
└──────────┘               │  - Weather    │
                           │  - Posts      │ JPA/MySQL
                           │  - AI Search  │ RestTemplate
                           │  - Explore    │ (Naver API)
                           └──────┬────────┘
                                  │
                                  │ REST (내부)
                                  ▼
                           ┌──────────────┐
                           │  크롤러 API  │ Flask/Gunicorn (5001)
                           │  - crawl_*   │ Transformers(BART)
                           │  - summarize │ MySQL write
                           └──────┬───────┘
                                  │ JDBC
                                  ▼
                           ┌──────────────┐
                           │    MySQL     │ (db:3306 / host:3307)
                           └──────────────┘
```

---

## 🧩 주요 기능

* **날씨**: 기상청 단기예보/초단기실황 기반 지역별 현재 상태 조회
* **공지/소식 수집**: 서산시청·읍면동·문화/관광 게시판 등 정기 크롤링
* **요약(AI)**: `gogamza/kobart-summarization` 모델로 TL;DR 및 문서별 1\~2문장 요약
* **탐색(네이버)**: 뉴스/블로그/카페 통합 검색 및 트렌드 분석 API
* **스케줄러**: 3시간 주기 자동 크롤링(`@Scheduled`)

---

## 🗂 레포 구조

```
seosan-issue/
├─ backend/                  # Spring Boot (Java 21)
│  ├─ src/main/java/org/likelionhsu/backend/
│  │  ├─ ai/                 # AI 오케스트레이션(요약, 프롬프트 빌드)
│  │  ├─ post/               # 게시글 도메인/조회 API
│  │  ├─ weather/            # KMA 연동 서비스
│  │  ├─ naversearch/        # 네이버 검색/트렌드 API
│  │  ├─ flask/              # 크롤러 연동 프록시
│  │  ├─ scheduler/          # 정기 크롤링 스케줄러
│  │  └─ common/config       # 외부 API/캐시 설정
│  └─ resources/
│     ├─ application.yml
│     └─ application-docker.yml
├─ crawler/                  # Flask + Transformers + MySQL
│  ├─ api.py                 # /health, /crawl*, /summarize, /content_stats
│  ├─ bart.py                # KoBART 요약 래퍼
│  └─ src/
│     ├─ crawlers/           # 서산시/읍면동/문화관광 크롤러
│     ├─ database.py         # 커넥션 풀/DDL/쿼리
│     └─ crawler_config.py   # 카테고리/URL/헤더 설정
├─ docker-compose.yml        # 3컨테이너 오케스트레이션
├─ render.yaml               # Render 배포 설정(사용 시)
├─ env.example               # 환경변수 템플릿
└─ README.md
```

---

## 🔧 환경 변수 (.env)

| 키                     | 설명                | 예시                           |
| --------------------- | ----------------- | ---------------------------- |
| `DB_PASSWORD`         | MySQL root 비밀번호   | `your_db_password_here`      |
| `DB_NAME`             | DB 스키마명           | `seosan_issue_db`            |
| `DB_USER`             | DB 유저             | `root`                       |
| `DB_HOST`             | DB 호스트            | `db` (도커) / `localhost` (로컬) |
| `NAVER_CLIENT_ID`     | 네이버 검색/트렌드 API    | `...`                        |
| `NAVER_CLIENT_SECRET` | 네이버 검색/트렌드 API    | `...`                        |
| `KMA_SERVICE_KEY`     | 기상청 API 서비스 키     | `...`                        |
| `PORT`                | 크롤러(Flask) 포트(선택) | `5001`                       |

> 도커 실행 시 `application-docker.yml`이 사용되며, 크롤러 URL 기본값은 `http://crawler:5001` 입니다.

---

## 🔌 백엔드 API (요청/응답 예시)

> 기본 프리픽스: `/api/v1`

### 1) 날씨 — `GET /api/v1/weather?region={name}`

**Query**: `region` (예: `서산시 전체`, `대산읍`, `인지면` … — `application.yml`의 `kma.api.grid-coords`에 정의)

**Response 예시**

```json
{
  "baseDate": "20250814",
  "baseTime": "1700",
  "region": "서산시 전체",
  "temperature": "29.6",
  "humidity": "72",
  "sky": "1",
  "pty": "0",
  "skyDescription": "맑음",
  "windSpeed": "1.1",
  "windDirection": "남남서"
}
```

### 2) 게시글 조회 — `GET /api/v1/posts`

**Query**: `region`(선택), `category`(선택, enum), `page`(기본 0), `size`(기본 10)
`category` 예: `CULTURE_NEWS`, `NOTICE`, `PRESS_RELEASE`, `WELFARE_YOUTH` …

**Response 예시 (Spring Page)**

```json
{
  "content": [
    { "id": 123, "title": "서산시 여름축제 안내", "pubDate": "2025.08.10", "region": "서산시 전체", "category": "CULTURE_NEWS" }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 231,
  "last": false,
  "totalPages": 24,
  "size": 10,
  "number": 0
}
```

### 3) 게시글 상세 — `GET /api/v1/posts/{postId}`

**Response 예시**

```json
{
  "id": 123,
  "title": "서산시 여름축제 안내",
  "content": "…본문 요약/정제…",
  "link": "https://www.seosan.go.kr/...",
  "pubDate": "2025.08.10",
  "region": "서산시 전체",
  "category": "CULTURE_NEWS",
  "department": "문화관광과",
  "views": 120,
  "crawledAt": "2025-08-10T12:34:56"
}
```

### 4) 네이버 탐색 — `GET /api/v1/explore/naver`

**Query**: `q`(필수), `types`(선택: `news,blog,cafearticle`), `display`(기본 10)

**Response 예시**

```json
[
  { "title": "<b>서산</b> 여름축제", "description": "…", "link": "https://news...", "type": "news" },
  { "title": "서산 맛집 모음", "description": "…", "link": "https://blog...", "type": "blog" }
]
```

### 5) 네이버 트렌드 — `POST /api/v1/naver-search/daily-trend`

**Query**: `startDate`, `endDate` (YYYY-MM-DD)
**Body 예시**

```json
[
  { "groupName": "축제", "keywords": ["서산 축제", "서산 행사"] }
]
```

### 6) AI 검색 요약 — `POST /api/v1/ai-search`

**Body**

```json
{ "query": "8월 서산 축제 일정 알려줘", "maxExternal": 3 }
```

**Response 예시**

```json
{
  "summary": "8월 서산 주요 축제는 … 주요 일정 … 요약.",
  "sources": [
    "https://www.seosan.go.kr/...",
    "https://news.naver.com/..."
  ]
}
```

### 7) 프롬프트 프리뷰 — `GET /api/v1/ai-search/preview?query=...&maxExternal=3`

**LLM 호출 없이** 요약 입력 프롬프트만 확인할 때 사용

---

## 🐍 크롤러 API (Flask)

> 기본 포트: `5001`

* `GET /health` — 헬스체크
* `GET /crawl_all` — 모든 카테고리 순회 크롤링 (페이지 수 제한 내부 기본값 적용)
* `GET /crawl?category={카테고리명}&pages={N}` — 특정 카테고리만 N페이지 크롤
* `GET /crawl_popular_terms` — 인기 검색어(일/주간) 샘플 제공
* `POST /summarize` — `{ "text": "..." }` 본문 요약 (KoBART)
* `GET /content_stats` — 금일 수집 건수/전일 대비 증감률

> 백엔드 프록시: `/flask/crawl_popular_terms`, `/flask/summarize` 등. (스케줄러는 3시간 주기 `/crawl_all` 트리거)

---

## 🗃 데이터 모델 (요약)

* **Post**

  * `id`, `title`, `content`, `link`, `pubDate`, `region`, `category`, `department`, `views`, `crawledAt`
* **Category(enum)**

  * `NEWS`, `NOTICE`, `PRESS_RELEASE`, `CULTURE_NEWS`, `CITY_TOUR`, `TOUR_GUIDE`,
  * 복지: `WELFARE_SENIOR`, `WELFARE_DISABLED`, `WELFARE_WOMEN_FAMILY`, `WELFARE_CHILD_YOUTH`, `WELFARE_YOUTH`
  * 기타: `HEALTH_WELLNESS`, `BLOG`, `CAFE` 등

---

## 🛠 개발 메모

* **캐시**: 로컬(무Redis) 환경에서 Caffeine 캐시로 대체 (`LocalCacheFallbackConfig`)
* **외부 API 설정**: `KmaApiConfig`, `FlaskClientConfig`로 프로퍼티 바인딩
* **스케줄링**: `CrawlScheduler`가 `/crawl_all`을 3시간마다 실행

---

## 🧪 테스트 & 품질 (추천 실행)

```bash
# 백엔드 테스트
cd backend
./gradlew test

# 크롤러 테스트
cd ../crawler
python run_tests.py
```

---

## 🧯 트러블슈팅

* **DB 접속 오류**: `.env`의 `DB_PASSWORD/DB_NAME/DB_USER/DB_HOST` 확인. 호스트포트는 `3307`.
* **KMA 403/키 에러**: `KMA_SERVICE_KEY` 재확인(디코딩/공백확인). 지역명은 `application.yml`의 `grid-coords`에 존재해야 함.
* **네이버 API 401**: `NAVER_CLIENT_ID/SECRET` 확인 및 사용량 제한 확인.
* **포트 충돌**: `docker-compose.yml`에서 포트 매핑 수정.

---
