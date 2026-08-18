FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY settings.xml pom.xml ./
RUN mvn -B -q -s /build/settings.xml dependency:go-offline
COPY src ./src
RUN mvn -B -q -s /build/settings.xml clean package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app \
    && mkdir -p /app/data/uploads \
    && chown -R app:app /app
WORKDIR /app
COPY --from=build /build/target/lishuiwan-api-1.0.0.jar app.jar
USER app
EXPOSE 80
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-Duser.timezone=Asia/Shanghai","-jar","/app/app.jar","--spring.profiles.active=prod"]
