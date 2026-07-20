# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copy only the build definition first. This layer stays cached until pom.xml
# or the wrapper changes, so editing source does not re-download dependencies.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# chmod is defensive: checkouts made on Windows can lose the executable bit.
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src

# Tests are skipped here so the image build stays fast; run them in CI or with
# ./mvnw test. Drop -DskipTests to have a failing test fail the image build.
RUN ./mvnw -B clean package -DskipTests

# Split the fat jar into layers that change at different rates. The glob avoids
# hardcoding the version, and does not match the *.jar.original left by
# spring-boot-maven-plugin.
#
# The extracted jar keeps the name of the jar it came from, so it is renamed to
# a fixed app.jar and the runtime ENTRYPOINT never has to know the version.
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination extracted \
    && mv extracted/application/*.jar extracted/application/app.jar

# ---------------------------------------------------------------------------
# Runtime stage
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Alpine has no useradd; adduser -S creates a system account. Nothing is
# installed here: the HEALTHCHECK below uses the wget that busybox already
# provides, since Alpine ships no curl.
RUN addgroup -S spring && adduser -S -u 10001 -G spring spring

# Every layer is copied into the SAME directory on purpose. The extracted
# app.jar is a thin jar whose manifest Class-Path points at ./lib, so the
# layers have to overlay into one tree rather than stay in separate folders.
# Ordering matters for cache reuse: least volatile first.
COPY --from=build --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/application/ ./

USER spring

EXPOSE 8080

# Java 21 reads container memory limits on its own; this just keeps the heap
# from crowding out the rest of the container.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -q -O - http://localhost:8080/api/health || exit 1

# exec form via sh so JAVA_OPTS expands, while exec keeps java as PID 1 so it
# still receives SIGTERM for a clean shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
