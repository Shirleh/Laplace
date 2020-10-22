FROM gradle:6.6-jdk11 AS builder
WORKDIR /app/
COPY build.gradle.kts /app/
COPY settings.gradle /app/
RUN gradle --refresh-dependencies
COPY /src/ /app/src/
RUN gradle shadowJar

FROM openjdk:11-jre-slim
LABEL maintainer="shirlehxo@protonmail.com"
COPY --from=builder app/build/libs/Laplace*.jar Laplace.jar
RUN mkdir /data/
ENTRYPOINT ["java", "-jar", "Laplace.jar"]
