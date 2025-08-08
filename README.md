# 📰 서산이슈 (Seosan-Issue)
> **AI 기반 충남 서산 지역 정보 통합·분석 플랫폼**  
서산시의 시정·복지·문화·뉴스를 자동으로 수집·요약·분류하여 제공하는 웹 애플리케이션

---

## 📌 프로젝트 개요
- **문제**: 서산시민들은 시청·읍면동 게시판·뉴스·블로그 등 흩어진 정보에 접근하기 어려움
- **해결**: AI 기반 자동 크롤링·요약·감정 분석·카테고리 분류로 정보 접근성 향상
- **핵심 목표**
  - 하루 단위의 정보 격차를 줄이는 **실시간 정보 허브**
  - 비전문 사용자도 쉽게 접근 가능한 **웹 기반 통합 플랫폼**
  - 공공데이터 + 민간 API 결합을 통한 **풍부한 데이터 소스**

---

## 🌟 주요 기능
1. **서산시청 소식 크롤링**
   - 시정소식, 고시공고 등 주요 게시판 자동 수집
   - BeautifulSoup 기반 HTML 파싱
   - 정해진 주기(3시간)마다 자동 실행 (Scheduler)

2. **네이버 뉴스·블로그 수집**
   - 네이버 검색 API로 `서산` 키워드 관련 최신 글 수집
   - 제목, 요약, 원문 링크 제공

3. **날씨 정보 제공**
   - 기상청 단기예보 API 기반
   - 읍면동별 실시간 날씨 데이터

4. **AI 분석**
   - GPT 기반 본문 요약
   - BERT 기반 감정 분석 (긍정/부정/중립)
   - 규칙 기반 카테고리 분류 (복지·문화·교통·경제·관광 등)

5. **RESTful API 제공**
   - 외부 서비스에서 재활용 가능
   - Swagger UI 기반 API 문서 제공

---

## 🛠 기술 스택
| 구분 | 기술 |
|------|------|
| **Backend** | Java 21, Spring Boot 3, Spring Data JPA, QueryDSL |
| **Crawler** | Python 3.11, Flask, BeautifulSoup4, Requests, Gunicorn |
| **AI 분석** | OpenAI GPT, HuggingFace Transformers (BERT) |
| **Database** | MySQL 8.0 |
| **빌드/배포** | Gradle, Docker, Docker Compose |
| **기상 API** | KMA(기상청 단기예보 API) |
| **검색 API** | Naver Search API |

---

## 🚀 시작하기

### 1. 사전 요구사항
- **공통**: Git
- **Docker 실행용**: Docker Desktop
- **로컬 개발용**:
  - Java 21 (JDK)
  - Python 3.11+
  - IntelliJ IDEA (또는 선호하는 Java IDE)
  - MySQL 8.0 설치

---

### 2. 환경 변수 설정
`.env` 파일 생성:
```bash
cp env.example .env

.env 예시:
DB_PASSWORD=my-secret-pw
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_secret
KMA_SERVICE_KEY=your_kma_service_key
OPENAI_API_KEY=your_openai_api_key

3. Docker로 전체 서비스 실행

docker compose up --build
Backend API: http://localhost:8082

Crawler API: http://localhost:5001

MySQL: localhost:3306

종료:

docker compose down
4. 로컬 개발 환경 실행
Backend 실행

로컬 MySQL에 DB 생성:

CREATE DATABASE seosan_issue_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
.env DB_PASSWORD와 로컬 DB 비밀번호 맞춤

IntelliJ에서 BackendApplication.java 실행 (dev 프로필)

Crawler 실행

cd crawler
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt

export DB_PASSWORD="your_db_password" && python api.py

📂 프로젝트 구조

seosanissue/
├── backend/             # Spring Boot 백엔드
│   └── src/
├── crawler/             # Python 크롤러
│   └── src/
├── docker-compose.yml   # 전체 서비스 구성
├── .env.example         # 환경 변수 템플릿
└── README.md


🌐 주요 API 엔드포인트
메서드	URL	설명
GET	/posts	게시글 목록 조회
GET	/posts/{id}	게시글 상세 조회
GET	/weather	날씨 조회
GET	/crawl?category={name}	특정 카테고리 크롤링 실행
