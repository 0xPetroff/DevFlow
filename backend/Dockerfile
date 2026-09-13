# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /build

# Resolved before the sources are copied so a code change does not re-resolve the tree.
# The cache mount survives between builds; the separate layer is what helps a cold builder.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B dependency:go-offline

COPY src ./src
# Tests need a Docker daemon for Testcontainers, which this build does not have. They run as
# their own pipeline stage against a real database instead.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests package

# Boot 4 dropped the layertools jarmode; `-Djarmode=tools extract --layers` replaces it.
# Splitting the jar is worth a stage: dependencies are 67 MB and the application 1 MB, so a
# code-only release rebuilds and re-uploads one thin layer.
FROM build AS layers
WORKDIR /layers
# The destination has to be empty, so the jar stays in target/ rather than being copied here.
RUN java -Djarmode=tools -jar /build/target/devflow-api-*.jar \
    extract --layers --launcher --destination /layers

FROM eclipse-temurin:25-jre-alpine AS runtime

# curl serves the container healthcheck. Busybox wget cannot fail on a 4xx, which is what an
# unready actuator probe returns.
RUN apk add --no-cache curl \
    && addgroup -S -g 10001 devflow \
    && adduser -S -u 10001 -G devflow -H -D devflow

WORKDIR /app
# Owned by root and read-only to the runtime user: the process never needs to write its own code.
COPY --from=layers /layers/dependencies/ ./
COPY --from=layers /layers/spring-boot-loader/ ./
COPY --from=layers /layers/snapshot-dependencies/ ./
COPY --from=layers /layers/application/ ./

USER 10001:10001
EXPOSE 8080

# The JVM sees the container limit, not the host's, so a percentage is the only portable heap
# setting. ExitOnOutOfMemoryError makes the orchestrator replace a dying container instead of
# leaving it up and failing every request.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
