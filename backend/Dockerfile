# 1단계: Gradle 빌드용 컨테이너
FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /app

# 프로젝트 루트에서 Gradle Wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle/
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
COPY settings.gradle .

# 백엔드 모듈의 build.gradle 파일 복사
COPY backend/build.gradle ./backend/build.gradle

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# Gradle 빌드 시 네트워크 타임아웃 설정
ENV GRADLE_OPTS="-Dorg.gradle.daemon.timeout=3600000"

# 먼저 의존성을 다운로드하여 Docker 캐시 활용 (타임아웃 2분으로 증가)
RUN ./gradlew -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000 :backend:dependencies --no-daemon

# 백엔드 모듈의 소스 코드 복사
COPY backend/src ./backend/src

# 실행 가능한 JAR 파일 빌드 (타임아웃 2분으로 증가)
RUN ./gradlew -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000 :backend:bootJar -x test --no-daemon

# 2단계: 경량 런타임 이미지 (JRE only)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Spring profile 설정 (환경 변수로)
ENV SPRING_PROFILES_ACTIVE=production

# 빌더 스테이지에서 생성된 JAR 파일 복사
COPY --from=builder /app/backend/build/libs/*.jar app.jar

# 애플리케이션 포트 노출
EXPOSE 8082

# 애플리케이션 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]