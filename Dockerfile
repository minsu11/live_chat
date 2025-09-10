# 1단계: JAR 파일만 실행하는 가벼운 이미지
FROM eclipse-temurin:21-jdk-alpine

# 컨테이너 내부 작업 디렉토리
WORKDIR /app

# Gradle 빌드 결과 JAR 복사
COPY build/libs/*.jar app.jar

# 컨테이너가 실행할 명령어
ENTRYPOINT ["java","-jar","/app/app.jar"]
