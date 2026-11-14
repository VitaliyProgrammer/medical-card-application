package com.vitaliy.medcard;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class IntegrationTestBase {

    private static final int MYSQL_STARTUP_TIMEOUT_SECONDS = 300;

    private static final String MINIO_IMAGE = "cgr.dev/chainguard/minio@"
            + "sha256:fbbc60f24cc4b293ef713d0d7c895ddf1d2ed54e6d4f9d7c1044eb840bff5a28";

    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withCommand("--innodb-buffer-pool-size=64M", "--innodb-redo-log-capacity=128M")
            .withStartupTimeoutSeconds(MYSQL_STARTUP_TIMEOUT_SECONDS);

    static final GenericContainer<?> MINIO_CONTAINER =
            new GenericContainer<>(MINIO_IMAGE)
                    .withExposedPorts(9000)
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withCommand("server", "/data")
                    .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    static {
        MYSQL_CONTAINER.start();
        MINIO_CONTAINER.start();
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("app.storage.endpoint", () -> "http://" + MINIO_CONTAINER.getHost()
                + ":" + MINIO_CONTAINER.getMappedPort(9000));
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }
}
