# 'gradle bootJar' before docker build !

# pull official base image
FROM eclipse-temurin:17-jre-alpine

# folder where jar file is build into
ARG JAR_FILE=build/libs/\*.jar

# copy JAR file
COPY ${JAR_FILE} app.jar

# exposing port
EXPOSE 8080

# define entry point
ENTRYPOINT ["java","-jar","/app.jar"]
