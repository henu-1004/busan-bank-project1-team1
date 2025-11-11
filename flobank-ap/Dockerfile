# 1. Java 21(Oracle) 이미지를 불러옵니다.
FROM eclipse-temurin:21-jdk-alpine

# 2. 작업 디렉터리를 /app으로 설정
WORKDIR /app

# 3. Gradle 빌드에 필요한 파일들을 먼저 복사 (캐시 활용)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src

# 4. gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 5. 프로젝트 빌드 (이제 소스 코드가 있으므로 성공)
RUN ./gradlew build -x test

# 6. 소스 코드는 더 이상 필요 없으니 삭제 (선택 사항이지만 권장)
# RUN rm -rf src

# 7. 실행가능한 JAR 파일만 app.jar로 복사
RUN find build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} app.jar \;

# 8. 이 Docker 이미지가 실행될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "/app/app.jar"]