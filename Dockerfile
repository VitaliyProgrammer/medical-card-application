FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY mvnw pom.xml checkstyle.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/*.jar application.jar \
    && java -Djarmode=layertools -jar application.jar extract --destination layers

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 --no-create-home medcard
WORKDIR /application
COPY --from=builder /build/layers/dependencies/ ./
COPY --from=builder /build/layers/spring-boot-loader/ ./
COPY --from=builder /build/layers/snapshot-dependencies/ ./
COPY --from=builder /build/layers/application/ ./
USER medcard
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
