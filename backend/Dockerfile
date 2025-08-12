#backend-Dockerfile
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# CRLF → LF 변환 및 실행권한 부여
RUN apk add --no-cache dos2unix
COPY gradlew .
RUN dos2unix gradlew && chmod +x gradlew

# Gradle wrapper & 설정 복사
COPY gradle gradle/
COPY settings.gradle .
COPY backend/build.gradle ./backend/build.gradle

# 의존성 캐싱
ENV GRADLE_OPTS="-Dorg.gradle.daemon.timeout=3600000"
RUN ./gradlew -Dorg.gradle.internal.http.connectionTimeout=120000 \
              -Dorg.gradle.internal.http.socketTimeout=120000 \
              :backend:dependencies --no-daemon

# 소스 복사 및 빌드
COPY backend/src ./backend/src
RUN ./gradlew -Dorg.gradle.internal.http.connectionTimeout=120000 \
              -Dorg.gradle.internal.http.socketTimeout=120000 \
              :backend:bootJar -x test --no-daemon

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=production

COPY .env .

COPY --from=builder /app/backend/build/libs/*.jar app.jar

# Render/로컬 모두 $PORT 지원 (기본 8083)
EXPOSE 8083
ENTRYPOINT ["sh","-c","sleep 10 && java -jar app.jar --server.port=${PORT:-8083}"]
