# 1. OpenJDK 17 slim 이미지 사용
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 애플리케이션 JAR 복사
COPY build/libs/youtil-0.0.1-SNAPSHOT.jar app.jar

# 4. OpenTelemetry Java Agent 다운로드
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# 5. 포트 설정
EXPOSE 8080

# 6. 실행 명령 - javaagent 적용
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]