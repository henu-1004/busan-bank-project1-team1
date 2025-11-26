# 1. 컨테이너 크기를 줄이기 위해 Java Runtime Environment (JRE)만 있는 이미지를 사용합니다.
FROM eclipse-temurin:21-jre-alpine

# 2. 작업 디렉터리를 /app으로 설정
WORKDIR /app

# 3. 배포 스크립트(deploy.yml)에서 복사해준 app.jar 파일을 컨테이너 안으로 복사합니다.
# Docker Compose는 이 Dockerfile을 빌드할 때 /opt/flobank-ap/app.jar 파일을 사용합니다.
COPY app.jar app.jar

# 4. 이 Docker 이미지가 실행될 때 실행할 명령어
# 복사된 app.jar을 실행합니다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]