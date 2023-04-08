# Use a multi-stage build to generate the JAR file and then package it into the Docker image
# docker buildx build -t imdb-clone-backend .
# docker run --name imdb-clone-backend -p 8080:8080 imdb-clone-backend:latest
FROM gradle:8.0.2-jdk19-jammy AS build
COPY --chown=gradle:gradle . /home/gradle/app
WORKDIR /home/gradle/app
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:19-jre-jammy
ARG JAR_FILE=/home/gradle/app/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]