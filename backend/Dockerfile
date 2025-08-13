#backend-Dockerfile (수정본)
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

RUN apk add --no-cache dos2unix
COPY gradlew .
RUN dos2unix gradlew && chmod +x gradlew

COPY gradle gradle/
COPY settings.gradle .
COPY backend/build.gradle ./backend/build.gradle

ENV GRADLE_OPTS="-Dorg.gradle.daemon.timeout=3600000"
RUN ./gradlew -Dorg.gradle.internal.http.connectionTimeout=120000 \
              -Dorg.gradle.internal.http.socketTimeout=120000 \
              :backend:dependencies --no-daemon

COPY backend/src ./backend/src
RUN ./gradlew -Dorg.gradle.internal.http.connectionTimeout=120000 \
              -Dorg.gradle.internal.http.socketTimeout=120000 \
              :backend:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

COPY .env .

COPY --from=builder /app/backend/build/libs/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["sh","-c","sleep 10 && java -jar app.jar --server.port=${PORT:-8083}"]
