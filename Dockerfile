FROM gradle:6.6-jdk11 AS builder
COPY build.gradle.kts /app/
COPY settings.gradle /app/
COPY /src/ /app/src/
WORKDIR /app/
RUN gradle shadowJar

FROM openjdk:11-jre-slim
LABEL maintainer="shirlehxo@protonmail.com"
COPY --from=builder app/build/libs/Laplace-1.0-SNAPSHOT-all.jar Laplace.jar
RUN mkdir /data/
ENTRYPOINT ["java", "-jar", "Laplace.jar"]
