# 1. OpenJDK 17 slim 이미지 사용
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# OpenTelemetry agent 다운로드
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /otelagent/opentelemetry-javaagent.jar

# 3. 애플리케이션 JAR 복사
COPY build/libs/youtil-0.0.1-SNAPSHOT.jar app.jar

# 5. 포트 설정
EXPOSE 8080

# 환경변수 설정 (실행 시에도 가능)
ENV OTEL_RESOURCE_ATTRIBUTES=service.name=youtil-be-dev \
    OTEL_EXPORTER_OTLP_ENDPOINT=https://ingest.us.signoz.cloud:443 \
    OTEL_EXPORTER_OTLP_HEADERS="signoz-ingestion-key=d3cd593a-688b-49f5-9ab0-314389b81d1a"


# 6. 실행 명령
CMD ["java", "-javaagent:/otelagent/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]