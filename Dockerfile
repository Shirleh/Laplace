# syntax = docker/dockerfile:1.0-experimental
FROM openjdk:11 AS builder
COPY /src/ /app/src/
COPY build.gradle.kts /app/
COPY gradle.properties /app/
COPY settings.gradle /app/
COPY gradlew /app/
COPY /gradle/ /app/gradle/
WORKDIR /app/
RUN ./gradlew shadowJar

FROM openjdk:11
LABEL maintainer="shirlehxo@protonmail.com"
COPY --from=builder /app/build/libs/Laplace-1.0-SNAPSHOT-all.jar /Laplace.jar
ENTRYPOINT ["java", "-jar", "/Laplace.jar"]
