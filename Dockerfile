FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle 래퍼와 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 다운로드를 위해 소스 코드 복사
COPY src src
# 실행 권한 부여 후 빌드 진행 (테스트 제외)
RUN chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon

# 런타임 이미지 (JRE만 포함하여 용량 최적화)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌더 스테이지에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 환경 변수 기본값 (compose에서 덮어쓰기 됨)
ENV TZ=Asia/Seoul

# 애플리케이션 포트 노출
EXPOSE 8080

# 컨테이너 실행 시 jar 파일 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
