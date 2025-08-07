# 서산이슈 (Seosan-Issue)

서산시의 다양한 소식(시정, 뉴스, 복지 등)을 수집하여 제공하는 웹 애플리케이션입니다.

## 🌟 주요 기능

- **서산시청 소식 크롤링**: 시정소식, 고시공고 등 서산시청의 주요 게시판을 주기적으로 크롤링하여 최신 정보를 수집합니다.
- **네이버 뉴스 검색**: 네이버 검색 API를 통해 '서산' 관련 키워드의 최신 소식을 수집합니다.
- **날씨 정보 제공**: 기상청 단기예보 API를 통해 서산시 각 지역의 날씨 정보를 제공합니다.
- **RESTful API**: 수집된 데이터를 외부에서 활용할 수 있도록 API를 제공합니다.

## 🛠️ 기술 스택

- **Backend**: Java, Spring Boot 3, Spring Data JPA, QueryDSL
- **Crawler**: Python, Flask, BeautifulSoup4, Gunicorn
- **Database**: MySQL 8.0
- **Build**: Gradle
- **Deployment**: Docker, Docker Compose

---

## 🚀 시작하기

이 프로젝트는 Docker를 이용한 전체 서비스 실행과, IDE를 이용한 로컬 개발 환경 실행을 모두 지원합니다.

### 1. 사전 요구사항

- **공통**: [Git](https://git-scm.com/)
- **Docker 실행용**: [Docker Desktop](https://www.docker.com/get-started)
- **로컬 개발용**: 
    - [Java 21 (JDK)](https://www.oracle.com/java/technologies/downloads/#java21)
    - [Python 3.11+](https://www.python.org/downloads/)
    - [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (또는 선호하는 Java IDE)
    - 로컬에 설치된 [MySQL 8.0](https://dev.mysql.com/downloads/mysql/)

### 2. 환경 변수 설정

프로젝트를 실행하기 위해 필요한 API 키와 데이터베이스 접속 정보를 설정해야 합니다.

1.  `env.example` 파일을 복사하여 `.env` 파일을 생성합니다.
    ```bash
    # PowerShell 또는 macOS/Linux
    cp env.example .env
    
    # Windows CMD
    copy env.example .env
    ```

2.  생성된 `.env` 파일을 열어 아래 내용을 자신의 환경에 맞게 수정합니다.

    | 변수명                  | 설명                                     | 예시                     |
    | ----------------------- | ---------------------------------------- | ------------------------ |
    | `DB_PASSWORD`           | 데이터베이스 `root` 계정의 비밀번호      | `my-secret-pw`           |
    | `NAVER_CLIENT_ID`       | 네이버 개발자 센터에서 발급받은 Client ID | `your_naver_client_id`   |
    | `NAVER_CLIENT_SECRET`   | 네이버 개발자 센터의 Client Secret        | `your_naver_secret`      |
    | `KMA_SERVICE_KEY`       | 공공데이터포털에서 발급받은 기상청 API 키 | `your_kma_service_key`   |

---

## 🐳 Docker로 전체 서비스 실행하기 (운영 환경)

가장 간편하게 모든 서비스를 실행하는 방법입니다.

1.  프로젝트 루트 디렉토리에서 아래 명령어를 실행합니다. 
    ```bash
    docker-compose up --build
    ```
    *최초 실행 시 이미지를 빌드하므로 시간이 다소 걸릴 수 있습니다.*

2.  서비스가 정상적으로 실행되면 각 엔드포인트에 접근할 수 있습니다.
    - **Backend API**: `http://localhost:8082`
    - **Crawler API**: `http://localhost:5001`
    - **Database**: `localhost:3306` (MySQL 클라이언트에서 접속 가능)

3.  서비스를 종료하려면 다음 명령어를 실행합니다.
    ```bash
    docker-compose down
    ```

---

## 💻 로컬에서 개발 환경 실행하기

백엔드 또는 크롤러를 개별적으로 수정하고 테스트할 때 유용한 방법입니다..

### Backend (Spring Boot) 실행

1.  **데이터베이스 준비**:
    - 로컬에 설치된 MySQL에 접속하여 `seosan_issue_db` 데이터베이스를 생성합니다.
      ```sql
      CREATE DATABASE seosan_issue_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
      ```
    - `.env` 파일의 `DB_PASSWORD`가 로컬 DB 비밀번호와 일치하는지 확인합니다.

2.  **IntelliJ에서 프로젝트 열기**:
    - IntelliJ에서 `seosanissue` 프로젝트를 엽니다.
    - `BackendApplication.java` 파일을 찾아 실행(Run)합니다.
    - Spring Boot는 `dev` 프로필로 실행되며, `application-dev.yml` 설정을 사용합니다.

### Crawler (Python) 실행

1.  **가상 환경 설정**:
    ```bash
    # /crawler 디렉토리로 이동
    cd crawler

    # 가상환경 생성
    python -m venv .venv

    # 가상환경 활성화 (Windows)
    .venv\Scripts\activate
    # 가상환경 활성화 (macOS/Linux)
    source .venv/bin/activate
    ```

2.  **의존성 설치**:
    ```bash
    pip install -r requirements.txt
    ```

3.  **크롤러 API 서버 실행**:
    - `.env` 파일의 `DB_PASSWORD`를 환경 변수로 설정하고 Flask 서버를 실행합니다.
    ```bash
    # Windows PowerShell
    $env:DB_PASSWORD="your_db_password"; python api.py

    # macOS/Linux
    export DB_PASSWORD="your_db_password" && python api.py
    ```

4.  **크롤링 트리거**: 
    - 크롤러 서버가 실행된 상태에서, 프로젝트 루트의 `trigger_crawl.py`를 실행하여 특정 카테고리의 크롤링을 시작할 수 있습니다.

---

## 📂 프로젝트 구조

```
.seosanissue/
├── backend/         # Spring Boot 백엔드 애플리케이션
│   ├── src/
│   └── build.gradle
├── crawler/         # Python 크롤러 애플리케이션
│   ├── src/
│   └── api.py
├── .github/         # GitHub Actions 워크플로우
├── docker-compose.yml # 전체 서비스 오케스트레이션
├── Dockerfile       # 각 서비스의 Docker 이미지 설정
├── .env.example     # 환경 변수 템플릿
└── README.md        # 프로젝트 문서
```

## 🌐 주요 API 엔드포인트

- **GET /posts**: 모든 게시글 목록을 조회합니다.
- **GET /posts/{id}**: 특정 ID의 게시글 상세 정보를 조회합니다.
- **GET /weather**: 현재 날씨 정보를 조회합니다.
- **GET /crawl?category={category_name}**: 특정 카테고리의 크롤링을 시작합니다. (크롤러 API)

*자세한 API 명세는 Postman 컬렉션이나 Swagger를 통해 확인할 수 있습니다. (추가 예정)*
