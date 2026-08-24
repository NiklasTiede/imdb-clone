# Use a multi-stage build to generate the JAR file and then package it into the Docker image
FROM gradle:9.5-jdk25-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/app
WORKDIR /home/gradle/app
RUN gradle bootJar copyPyroscopeAgent --no-daemon

# best to keep JDK and JRE version same
FROM eclipse-temurin:25-jre-alpine
ARG JAR_FILE=/home/gradle/app/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
COPY --from=build /home/gradle/app/build/pyroscope/pyroscope.jar /opt/pyroscope/pyroscope.jar
COPY --chmod=0555 scripts/backend-entrypoint /usr/local/bin/backend-entrypoint
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/backend-entrypoint"]
