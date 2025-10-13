# ---- Build stage ----
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Gradle 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사
COPY src src

# Gradle 빌드 실행
RUN chmod +x gradlew
RUN ./gradlew build -x test

# ---- Runtime stage ----
FROM openjdk:21-slim

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 9003

# 애플리케이션 실행
CMD ["java", "-jar", "app.jar"]
