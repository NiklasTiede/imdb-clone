# Use a multi-stage build to generate the JAR file and then package it into the Docker image
FROM gradle:8.3-jdk20-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/app
WORKDIR /home/gradle/app
RUN gradle bootJar --no-daemon

# best to keep JDK and JRE version same
#FROM eclipse-temurin:20-jre-alpine
FROM eclipse-temurin:20-jre-alpine
ARG JAR_FILE=/home/gradle/app/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]