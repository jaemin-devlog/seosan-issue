# 서산 이슈 백엔드 API 명세서

## 목차
1. [사용자 인증 API](#1-사용자-인증-api)
2. [게시글 API](#2-게시글-api)
3. [댓글 API](#3-댓글-api)
4. [북마크 API](#4-북마크-api)
5. [좋아요 API](#5-좋아요-api)
6. [마이페이지 API](#6-마이페이지-api)
7. [네이버 검색 API](#7-네이버-검색-api)
8. [탐색(Explore) API](#8-탐색explore-api)
9. [크롤러 API](#9-크롤러-api)

---

## 1. 사용자 인증 API

### 1.1 회원가입
```
POST /api/users/signup
```

**Request Body**
```json
{
  "username": "string",
  "password": "string",
  "email": "string"
}
```

**Response** (201 Created)
```json
{
  "id": "number",
  "username": "string",
  "email": "string"
}
```

---

### 1.2 로그인
```
POST /api/users/login
```

**Request Body**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response** (200 OK)
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

---

### 1.3 토큰 갱신
```
POST /api/users/refresh
```

**Request Body**
```json
{
  "refreshToken": "string"
}
```

**Response** (200 OK)
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

---

### 1.4 내 정보 조회
```
GET /api/users/me
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** (200 OK)
```json
{
  "id": "number",
  "username": "string",
  "email": "string"
}
```

---

### 1.5 회원 탈퇴
```
DELETE /api/users/me
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** (204 No Content)

---

## 2. 게시글 API

### 2.1 게시글 목록 조회 (필터링)
```
GET /api/posts
```

**Query Parameters**
- `region` (optional): 지역 필터 (예: "서산시 전체", "대산읍")
- `category` (optional): 카테고리 필터 (예: "LOCAL_NEWS", "EVENT", "WELFARE")
- `page` (optional, default: 0): 페이지 번호
- `size` (optional, default: 10): 페이지 크기

**Response** (200 OK)
```json
{
  "content": [
    {
      "id": "number",
      "title": "string",
      "content": "string",
      "region": "string",
      "category": "string",
      "link": "string",
      "viewCount": "number",
      "likeCount": "number",
      "commentCount": "number",
      "createdAt": "string (ISO 8601)"
    }
  ],
  "pageable": {
    "pageNumber": "number",
    "pageSize": "number"
  },
  "totalElements": "number",
  "totalPages": "number",
  "last": "boolean"
}
```

---

### 2.2 게시글 상세 조회
```
GET /api/posts/{postId}
```

**Path Parameters**
- `postId`: 게시글 ID

**Response** (200 OK)
```json
{
  "id": "number",
  "title": "string",
  "content": "string",
  "region": "string",
  "category": "string",
  "link": "string",
  "viewCount": "number",
  "likeCount": "number",
  "commentCount": "number",
  "isBookmarked": "boolean",
  "isLiked": "boolean",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

---

## 3. 댓글 API

### 3.1 댓글 목록 조회
```
GET /api/posts/{postId}/comments
```

**Path Parameters**
- `postId`: 게시글 ID

**Response** (200 OK)
```json
[
  {
    "id": "number",
    "content": "string",
    "author": {
      "id": "number",
      "username": "string"
    },
    "createdAt": "string (ISO 8601)",
    "updatedAt": "string (ISO 8601)"
  }
]
```

---

### 3.2 댓글 작성
```
POST /api/posts/{postId}/comments
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID

**Request Body**
```json
{
  "content": "string"
}
```

**Response** (200 OK)
```json
{
  "id": "number",
  "content": "string",
  "author": {
    "id": "number",
    "username": "string"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

---

### 3.3 댓글 수정
```
PUT /api/posts/{postId}/comments/{commentId}
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID
- `commentId`: 댓글 ID

**Request Body**
```json
{
  "content": "string"
}
```

**Response** (200 OK)
```json
{
  "id": "number",
  "content": "string",
  "author": {
    "id": "number",
    "username": "string"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

---

### 3.4 댓글 삭제
```
DELETE /api/posts/{postId}/comments/{commentId}
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID
- `commentId`: 댓글 ID

**Response** (204 No Content)

---

## 4. 북마크 API

### 4.1 북마크 추가
```
POST /api/posts/{postId}/bookmarks
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID

**Response** (200 OK)

---

### 4.2 북마크 제거
```
DELETE /api/posts/{postId}/bookmarks
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID

**Response** (204 No Content)

---

## 5. 좋아요 API

### 5.1 게시글 좋아요
```
POST /api/posts/{postId}/likes
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID

**Response** (200 OK)

---

### 5.2 게시글 좋아요 취소
```
DELETE /api/posts/{postId}/likes
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `postId`: 게시글 ID

**Response** (204 No Content)

---

## 6. 마이페이지 API

### 6.1 내가 북마크한 게시글 조회
```
GET /api/users/me/bookmarks
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
- `page` (optional, default: 0): 페이지 번호
- `size` (optional, default: 10): 페이지 크기

**Response** (200 OK)
```json
{
  "content": [
    {
      "id": "number",
      "title": "string",
      "region": "string",
      "category": "string",
      "createdAt": "string (ISO 8601)"
    }
  ],
  "pageable": {
    "pageNumber": "number",
    "pageSize": "number"
  },
  "totalElements": "number",
  "totalPages": "number",
  "last": "boolean"
}
```

---

### 6.2 내가 좋아요한 게시글 조회
```
GET /api/users/me/likes
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
- `page` (optional, default: 0): 페이지 번호
- `size` (optional, default: 10): 페이지 크기

**Response** (200 OK)
```json
{
  "content": [
    {
      "id": "number",
      "title": "string",
      "region": "string",
      "category": "string",
      "createdAt": "string (ISO 8601)"
    }
  ],
  "pageable": {
    "pageNumber": "number",
    "pageSize": "number"
  },
  "totalElements": "number",
  "totalPages": "number",
  "last": "boolean"
}
```

---

### 6.3 내가 작성한 댓글 조회
```
GET /api/users/me/comments
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
- `page` (optional, default: 0): 페이지 번호
- `size` (optional, default: 10): 페이지 크기

**Response** (200 OK)
```json
{
  "content": [
    {
      "id": "number",
      "content": "string",
      "postId": "number",
      "postTitle": "string",
      "createdAt": "string (ISO 8601)"
    }
  ],
  "pageable": {
    "pageNumber": "number",
    "pageSize": "number"
  },
  "totalElements": "number",
  "totalPages": "number",
  "last": "boolean"
}
```

---

## 7. 네이버 검색 API

### 7.1 일별 트렌드 조회
```
POST /api/v1/naver-search/daily-trend
```

**Query Parameters**
- `startDate`: 시작 날짜 (YYYY-MM-DD)
- `endDate`: 종료 날짜 (YYYY-MM-DD)

**Request Body**
```json
[
  {
    "groupName": "string",
    "keywords": ["keyword1", "keyword2"]
  }
]
```

**Response** (200 OK)
```json
{
  "startDate": "string",
  "endDate": "string",
  "results": [
    {
      "title": "string",
      "keywords": ["string"],
      "data": [
        {
          "period": "string",
          "ratio": "number"
        }
      ]
    }
  ]
}
```

---

### 7.2 주별 트렌드 조회
```
POST /api/v1/naver-search/weekly-trend
```

**Query Parameters**
- `startDate`: 시작 날짜 (YYYY-MM-DD)
- `endDate`: 종료 날짜 (YYYY-MM-DD)

**Request Body**
```json
[
  {
    "groupName": "string",
    "keywords": ["keyword1", "keyword2"]
  }
]
```

**Response** (200 OK)
```json
{
  "startDate": "string",
  "endDate": "string",
  "results": [
    {
      "title": "string",
      "keywords": ["string"],
      "data": [
        {
          "period": "string",
          "ratio": "number"
        }
      ]
    }
  ]
}
```

---

## 8. 탐색(Explore) API

### 8.1 네이버 검색 결과 조회
```
GET /api/v1/explore/naver
```

**Query Parameters**
- `q`: 검색 키워드 (필수)
- `types` (optional): 검색 타입 배열 (기본값: ["news","blog","cafearticle"])
- `display` (optional, default: 10): 표시 개수

**Response** (200 OK)
```json
[
  {
    "title": "string",
    "description": "string",
    "link": "string",
    "pubDate": "string",
    "type": "string"
  }
]
```

---

### 8.2 단일 URL 요약
```
POST /api/v1/explore/summary
```

**Request Body**
```json
{
  "url": "string"
}
```

**Response** (200 OK)
```json
{
  "url": "string",
  "title": "string",
  "summary": "string",
  "keywords": ["string"]
}
```

**Response** (204 No Content) - 요약 불가능한 경우

---

### 8.3 배치 URL 요약
```
POST /api/v1/explore/summary/batch
```

**Request Body**
```json
{
  "urls": ["string", "string"]
}
```

**Response** (200 OK)
```json
{
  "items": [
    {
      "url": "string",
      "title": "string",
      "summary": "string",
      "keywords": ["string"]
    }
  ]
}
```

---

## 9. 크롤러 API

### 9.1 전체 카테고리 크롤링
```
GET /flask/crawl_all
```

**Query Parameters**
- `pages` (optional, default: 2): 크롤링할 페이지 수

**Response** (200 OK)
```json
{
  "ok": true,
  "total_new": "number",
  "summary": [
    {
      "category": "string",
      "new": "number"
    }
  ]
}
```

---

### 9.2 콘텐츠 통계 조회
```
GET /flask/content_stats
```

**Response** (200 OK)
```json
{
  "totalPosts": "number",
  "categoryCounts": {
    "category1": "number",
    "category2": "number"
  }
}
```

---

### 9.3 인기 검색어 크롤링
```
GET /flask/crawl_popular_terms
```

**Response** (200 OK)
```json
{
  "terms": ["string"],
  "crawledAt": "string (ISO 8601)"
}
```

---

### 9.4 텍스트 요약
```
POST /flask/summarize
```

**Request Body**
```json
{
  "text": "string",
  "maxLength": "number (optional)"
}
```

**Response** (200 OK)
```json
{
  "summary": "string",
  "originalLength": "number",
  "summaryLength": "number"
}
```

---

## 인증 방식

API는 JWT (JSON Web Token) 기반 인증을 사용합니다.

1. 로그인 후 받은 `accessToken`을 사용
2. Authorization 헤더에 Bearer 토큰 형식으로 포함
   ```
   Authorization: Bearer {accessToken}
   ```
3. 토큰 만료 시 `/api/users/refresh` 엔드포인트를 통해 갱신

---

## CORS 정책

- 크롤러 API: 모든 origin 허용
- 백엔드 API: 설정된 origin만 허용 (프론트엔드 도메인)

---

## 에러 응답 형식

```json
{
  "timestamp": "string (ISO 8601)",
  "status": "number",
  "error": "string",
  "message": "string",
  "path": "string"
}
```

일반적인 HTTP 상태 코드:
- 200: 성공
- 201: 생성 성공
- 204: 성공 (응답 본문 없음)
- 400: 잘못된 요청
- 401: 인증 실패
- 403: 권한 없음
- 404: 리소스를 찾을 수 없음
- 500: 서버 에러

---

## 카테고리 값

게시글 카테고리는 다음 값 중 하나를 사용합니다:
- `LOCAL_NEWS`: 지역 소식
- `EVENT`: 행사/이벤트
- `WELFARE`: 복지
- `CULTURE`: 문화
- `TRAFFIC`: 교통
- `EDUCATION`: 교육
- `EMPLOYMENT`: 취업
- `ETC`: 기타

---

## 지역 값

서산시 행정구역:
- 서산시 전체
- 대산읍
- 인지면
- 부석면
- 팔봉면
- 지곡면
- 성연면
- 음암면
- 운산면
- 해미면
- 고북면
- 동문1동
- 동문2동
- 수석동
- 석남동

