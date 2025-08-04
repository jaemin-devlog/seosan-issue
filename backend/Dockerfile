# 1단계: Gradle을 사용하여 애플리케이션 빌드
FROM gradle:8.7.0-jdk21-alpine AS builder

WORKDIR /app

# 프로젝트 전체를 먼저 복사합니다.
# 네트워크 문제로 개별 파일 다운로드에 실패하는 경우 이 방식이 더 안정적일 수 있습니다.
COPY . .

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 테스트를 제외하고 실행 가능한 JAR 파일 빌드
# 이 과정에서 Gradle 다운로드, 의존성 다운로드, 빌드가 모두 수행됩니다.
RUN ./gradlew :backend:bootJar -x test --no-daemon

# 2단계: 경량 런타임 이미지 생성
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일을 복사
COPY --from=builder /app/backend/build/libs/*.jar app.jar

# 애플리케이션이 사용하는 포트 노출
EXPOSE 8082

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
