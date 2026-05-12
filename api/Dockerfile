# =====================================================================
# ZOOM NEAR backend (Spring Boot 3.x / Java 21) — Render 배포용
# Multi-stage build: gradle 으로 fat jar 생성 → 작은 JRE 런타임에 복사
# =====================================================================

# 1) 빌드 스테이지
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper + 설정 먼저 복사 → 의존성 캐시 효율
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 + 빌드
COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# 2) 런타임 스테이지
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드된 jar 만 복사 (이름은 build.gradle.kts 의 archiveBaseName 따라감)
COPY --from=build /app/build/libs/*.jar app.jar

# Render 는 PORT 환경변수로 바인딩 포트를 지정 (Spring 의 server.port: ${PORT:8080} 가 받음)
EXPOSE 8080

# 운영 프로필 기본값. Render 환경변수에서 SPRING_PROFILES_ACTIVE=supabase 명시.
ENV SPRING_PROFILES_ACTIVE=supabase

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
