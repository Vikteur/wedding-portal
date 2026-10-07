# syntax=docker/dockerfile:1
# Fast-jar image, architecture-conventions §14.5: Gradle wrapper on a JDK 25 image, fast-jar on the Java 25 JRE.

# ---- build stage: the checked-in Gradle wrapper, tests skipped ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Wrapper first, so the pinned Gradle distribution is resolved once and cached.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --version

# Then the modules (.dockerignore lets in only build scripts and src/main).
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :application:quarkusBuild -x test -x integrationTest

# ---- runtime stage: the fast-jar on the JRE, as uid 10001 ----
FROM eclipse-temurin:25-jre AS runtime

# curl is only here for the HEALTHCHECK.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid 10001 --create-home --home-dir /home/app app

WORKDIR /app
COPY --from=build /build/application/build/quarkus-app/lib/ /app/lib/
COPY --from=build /build/application/build/quarkus-app/*.jar /app/
COPY --from=build /build/application/build/quarkus-app/app/ /app/app/
COPY --from=build /build/application/build/quarkus-app/quarkus/ /app/quarkus/

USER 10001:10001
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Duser.timezone=UTC" \
    QUARKUS_HTTP_HOST=0.0.0.0

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS --max-time 3 http://127.0.0.1:8080/api/health || exit 1

# exec makes java PID 1.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
