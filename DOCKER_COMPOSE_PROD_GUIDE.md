# 🚀 Docker Compose 프로덕션 배포 가이드

## docker-compose.prod.yml 실행 방법

### 1️⃣ 전제 조건

- Docker 설치 완료
- Docker Compose V2 설치 완료
- DockerHub에 이미지가 푸시되어 있어야 함

### 2️⃣ 환경변수 설정

`.env` 파일을 프로젝트 루트에 생성:

```bash
# 데이터베이스
DB_PASSWORD=your_secure_password
DB_NAME=seosan_issue_db
DB_USER=root
DB_HOST=db

# JWT
JWT_SECRET=your_jwt_secret_minimum_32_characters_long

# 네이버 API
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# 기상청 API (선택)
KMA_SERVICE_KEY=your_kma_service_key

# DockerHub 사용자명 (이미지 가져오기용)
DOCKERHUB_USERNAME=your_dockerhub_username
```

### 3️⃣ 실행 명령어

#### Windows (PowerShell)
```powershell
# 프로젝트 루트 디렉토리로 이동
cd C:\Users\이여재\Desktop\SpringProject\likelionProject\seosan-issue

# 프로덕션 환경 실행 (백그라운드)
docker compose -f docker-compose.prod.yml up -d

# 로그 확인
docker compose -f docker-compose.prod.yml logs -f

# 특정 서비스만 로그 확인
docker compose -f docker-compose.prod.yml logs -f backend
```

#### Linux/macOS (Bash)
```bash
# 프로젝트 루트 디렉토리로 이동
cd ~/seosan-issue

# 프로덕션 환경 실행 (백그라운드)
docker compose -f docker-compose.prod.yml up -d

# 로그 확인
docker compose -f docker-compose.prod.yml logs -f
```

### 4️⃣ 서비스 확인

```bash
# 실행 중인 컨테이너 확인
docker compose -f docker-compose.prod.yml ps

# 헬스체크
curl http://localhost:8083/api/posts
curl http://localhost:5001/health
```

### 5️⃣ 중지/재시작

```powershell
# 중지 (컨테이너만 중지, 데이터는 유지)
docker compose -f docker-compose.prod.yml stop

# 재시작
docker compose -f docker-compose.prod.yml start

# 완전 종료 (컨테이너 삭제, 데이터는 유지)
docker compose -f docker-compose.prod.yml down

# 완전 삭제 (볼륨까지 삭제 - 데이터 삭제됨!)
docker compose -f docker-compose.prod.yml down -v
```

---

## 📊 포트 매핑

| 서비스 | 호스트 포트 | 컨테이너 포트 | 설명 |
|--------|-------------|----------------|------|
| **MySQL** | 3307 | 3306 | 데이터베이스 |
| **Backend** | 8083 | 8083 | Spring Boot API |
| **Crawler** | 5001 | 5001 | Flask 크롤러 |

---

## 🔍 트러블슈팅

### 문제 1: 이미지를 찾을 수 없음

```
Error: pull access denied for username/seosan-issue-backend
```

**해결방법**:
1. DockerHub에 이미지가 푸시되어 있는지 확인
2. `.env` 파일의 `DOCKERHUB_USERNAME` 확인
3. DockerHub 로그인:
   ```bash
   docker login
   ```

### 문제 2: DB 연결 실패

```
backend | Cannot connect to database
```

**해결방법**:
1. MySQL 컨테이너가 정상 실행 중인지 확인:
   ```bash
   docker compose -f docker-compose.prod.yml ps db
   ```
2. 헬스체크 로그 확인:
   ```bash
   docker compose -f docker-compose.prod.yml logs db
   ```
3. `.env` 파일의 `DB_PASSWORD` 확인

### 문제 3: 포트 충돌

```
Error: port is already allocated
```

**해결방법**:
1. 기존 서비스 종료:
   ```bash
   docker compose -f docker-compose.yml down
   ```
2. 또는 `docker-compose.prod.yml`에서 포트 변경:
   ```yaml
   backend:
     ports: ["8084:8083"]  # 호스트 포트를 8084로 변경
   ```

---

## 🔒 보안 체크리스트

- [ ] `.env` 파일을 `.gitignore`에 추가
- [ ] 강력한 `DB_PASSWORD` 설정 (최소 16자, 대소문자+숫자+특수문자)
- [ ] 강력한 `JWT_SECRET` 설정 (최소 32자)
- [ ] 프로덕션 환경에서는 HTTPS 사용 (Nginx/Traefik)
- [ ] 방화벽 설정 (3307, 5001 포트 외부 접근 차단)
- [ ] 정기적인 백업 설정 (`db-data` 볼륨)

---

## 📦 이미지 빌드 및 푸시 (최초 1회)

### Backend 이미지
```bash
cd backend
docker build -t your_username/seosan-issue-backend:latest .
docker push your_username/seosan-issue-backend:latest
```

### Crawler 이미지
```bash
cd crawler
docker build -t your_username/seosan-issue-crawler:latest .
docker push your_username/seosan-issue-crawler:latest
```

---

## 🔄 업데이트 배포

새 버전을 배포할 때:

```bash
# 1. 새 이미지를 DockerHub에 푸시

# 2. 기존 컨테이너 중지
docker compose -f docker-compose.prod.yml stop backend crawler

# 3. 새 이미지 가져오기
docker compose -f docker-compose.prod.yml pull backend crawler

# 4. 재시작
docker compose -f docker-compose.prod.yml up -d backend crawler

# 5. 로그 확인
docker compose -f docker-compose.prod.yml logs -f backend
```

또는 한 번에:
```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

---

## 📝 로그 관리

### 로그 크기 제한 (선택)

`docker-compose.prod.yml`에 추가:

```yaml
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 로그 실시간 모니터링

```bash
# 모든 서비스
docker compose -f docker-compose.prod.yml logs -f

# 특정 서비스만
docker compose -f docker-compose.prod.yml logs -f backend

# 최근 100줄만
docker compose -f docker-compose.prod.yml logs --tail=100 backend

# 특정 시간 이후 로그
docker compose -f docker-compose.prod.yml logs --since 2025-12-03T10:00:00 backend
```

---

## 💾 백업 및 복구

### 데이터베이스 백업

```bash
# 백업
docker exec seosan-mysql-db mysqldump \
  -u root -p${DB_PASSWORD} \
  seosan_issue_db > backup_$(date +%Y%m%d).sql

# 복구
docker exec -i seosan-mysql-db mysql \
  -u root -p${DB_PASSWORD} \
  seosan_issue_db < backup_20251203.sql
```

### 볼륨 백업

```bash
# 볼륨 위치 확인
docker volume inspect seosan-issue_db-data

# 볼륨 백업 (tar)
docker run --rm \
  -v seosan-issue_db-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/db-backup.tar.gz -C /data .

# 볼륨 복구
docker run --rm \
  -v seosan-issue_db-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/db-backup.tar.gz -C /data
```

---

## 🎯 빠른 명령어 모음

```bash
# 실행
docker compose -f docker-compose.prod.yml up -d

# 중지
docker compose -f docker-compose.prod.yml stop

# 재시작
docker compose -f docker-compose.prod.yml restart

# 로그 (실시간)
docker compose -f docker-compose.prod.yml logs -f

# 상태 확인
docker compose -f docker-compose.prod.yml ps

# 완전 종료 (컨테이너 삭제)
docker compose -f docker-compose.prod.yml down

# 이미지 업데이트 + 재시작
docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d
```

---

**작성일**: 2025-12-03  
**문서 버전**: 1.0

