FROM maven:3-eclipse-temurin-25 AS builder

WORKDIR /usr/src/bot
COPY src src
COPY pom.xml pom.xml
RUN mvn clean package

FROM eclipse-temurin:25.0.2_10-jre

ENV RUN_IN_DOCKER=true
ENV TZ=Europe/Berlin
ENV CONFIG_PATH=/usr/src/bot/data/config.json

WORKDIR /usr/src/bot
COPY --from=builder /usr/src/bot/target/mensabot-*-jar-with-dependencies.jar mensabot.jar

VOLUME /usr/src/bot/data

ENTRYPOINT java -jar /usr/src/bot/mensabot.jar
