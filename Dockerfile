FROM eclipse-temurin:24-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY muffin-wallet-server/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
