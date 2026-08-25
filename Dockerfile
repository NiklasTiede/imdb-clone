# Use a multi-stage build to generate the JAR file and then package it into the Docker image
FROM gradle:9.5-jdk25-alpine AS build
WORKDIR /home/gradle/app
COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties gradle.lockfile gradlew ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle src ./src
RUN gradle bootJar copyPyroscopeAgent --no-daemon --no-watch-fs

# Keep JDK and JRE versions aligned and run the application as an unprivileged user.
FROM eclipse-temurin:25-jre-alpine
ARG JAR_FILE=/home/gradle/app/build/libs/*.jar
RUN addgroup -S -g 10001 imdb \
    && adduser -S -D -H -u 10001 -G imdb imdb
WORKDIR /app
COPY --from=build --chown=10001:10001 ${JAR_FILE} app.jar
COPY --from=build --chown=10001:10001 /home/gradle/app/build/pyroscope/pyroscope.jar /opt/pyroscope/pyroscope.jar
COPY --chmod=0555 scripts/backend-entrypoint /usr/local/bin/backend-entrypoint
USER 10001:10001
EXPOSE 8080 8081
ENTRYPOINT ["/usr/local/bin/backend-entrypoint"]
