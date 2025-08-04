# 서산이슈 (Seosan-Issue)

서산시의 다양한 소식(시정, 뉴스, 복지 등)을 크롤링하여 제공하는 웹 애플리케이션입니다.

## 주요 기능

- **서산시청 소식 크롤링**: 시정소식, 고시공고 등 서산시청의 주요 게시판을 주기적으로 크롤링하여 최신 정보를 수집합니다.
- **날씨 정보 제공**: 기상청 API를 통해 서산시의 날씨 정보를 제공합니다.
- **네이버 검색 결과 수집**: 네이버 검색 API를 통해 '서산' 관련 키워드의 최신 소식을 수집합니다.
- **게시판 기능**: 사용자들이 정보를 공유할 수 있는 게시판 기능을 제공합니다.

## 기술 스택

- **Backend**: Java, Spring Boot 3, Spring Data JPA
- **Crawler**: Python, Flask, BeautifulSoup4
- **Database**: MySQL 8.0
- **Build**: Gradle
- **Deployment**: Docker, Docker Compose

## 실행 방법

이 프로젝트는 Docker와 Docker Compose를 사용하여 모든 서비스를 한 번에 실행할 수 있도록 구성되어 있습니다.

### 사전 요구사항

- [Docker](https://www.docker.com/get-started)가 설치되어 있어야 합니다.

### 실행 절차

1.  **GitHub 레포지토리 클론**
    ```bash
    git clone https://github.com/your-github-username/seosanissue.git
    cd seosanissue
    ```

2.  **환경 변수 설정**
    프로젝트 루트에 있는 `env.example` 파일을 복사하여 `.env` 파일을 생성합니다.
    ```bash
    # Windows (CMD)
    copy env.example .env

    # Windows (PowerShell) / macOS / Linux
    cp env.example .env
    ```
    생성된 `.env` 파일을 열어, `your_..._here`로 표시된 부분에 실제 사용하시는 API 키와 데이터베이스 비밀번호를 입력합니다.

3.  **Docker Compose 실행**
    프로젝트 루트 디렉토리에서 다음 명령어를 실행합니다. Docker가 각 서비스의 이미지를 빌드하고 컨테이너를 실행하기 때문에, 최초 실행 시에는 시간이 다소 걸릴 수 있습니다.
    ```bash
    docker-compose up --build
    ```

4.  **확인**
    - **백엔드 API**: `http://localhost:8082` 에서 실행됩니다.
    - **크롤러 API**: `http://localhost:5001` 에서 실행됩니다.
    - **데이터베이스**: `localhost:3306` 에서 접근 가능합니다.

### 서비스 종료

모든 서비스를 중지하고 컨테이너를 삭제하려면 다음 명령어를 실행합니다.
```bash
docker-compose down
```
