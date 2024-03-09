FROM amazoncorretto:21.0.2-alpine3.19 as JDK

LABEL authors="Thiago Rigonatti"

WORKDIR /app

COPY gradle/ ./gradle/
COPY src/ ./src/
COPY settings.gradle .
COPY build.gradle .
COPY gradlew .

RUN ./gradlew update-version
RUN ./gradlew shadowJar

ENTRYPOINT exec java $JVM_OPTIONS -jar build/libs/rinha-de-backend-java-core-0.0.5-all.jar