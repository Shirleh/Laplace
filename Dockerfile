FROM gradle:6.5-jdk11 AS builder
COPY /src/ /app/src/
COPY build.gradle.kts /app/
COPY gradle.properties /app/
COPY settings.gradle /app/
WORKDIR /app/
RUN gradle shadowJar --no-daemon

FROM openjdk:11-jre-slim
LABEL maintainer="shirlehxo@protonmail.com"
COPY --from=builder /app/build/libs/Laplace-1.0-SNAPSHOT-all.jar /Laplace.jar
ENTRYPOINT ["java", "-jar", "/Laplace.jar"]
