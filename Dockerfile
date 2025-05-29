# 1. OpenJDK 17 slim 이미지 사용
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. JAR 복사
COPY build/libs/youtil-0.0.1-SNAPSHOT.jar app.jar

# 4. 포트 설정
EXPOSE 8000

# 5. 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]