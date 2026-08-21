ARG MAVEN_BASE_IMAGE=public.ecr.aws/docker/library/maven:3.9.16-amazoncorretto-17-alpine
ARG JAVA_RUNTIME_IMAGE=public.ecr.aws/docker/library/amazoncorretto:17-alpine

FROM ${MAVEN_BASE_IMAGE} AS build
WORKDIR /build
COPY settings.xml pom.xml ./
RUN mvn -B -q -s /build/settings.xml dependency:go-offline
COPY src ./src
RUN mvn -B -q -s /build/settings.xml clean package

FROM ${JAVA_RUNTIME_IMAGE}
RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S -D -H -G app app \
    && mkdir -p /app/data/uploads \
    && chown -R app:app /app
WORKDIR /app
COPY --from=build /build/target/lishuiwan-api-1.0.0.jar app.jar
USER app
EXPOSE 80
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-Duser.timezone=Asia/Shanghai","-jar","/app/app.jar","--spring.profiles.active=prod"]
