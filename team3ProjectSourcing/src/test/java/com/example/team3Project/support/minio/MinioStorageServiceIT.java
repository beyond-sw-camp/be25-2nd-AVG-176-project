package com.example.team3Project.support.minio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Docker가 있을 때만 실행됨({@code disabledWithoutDocker}). 로컬 MinIO 수동 기동 없이 연결·업로드 스모크 검증.
 */
@SpringBootTest(
        classes = { MinioConfig.class, MinioStorageService.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)

class MinioStorageServiceIT {

    @Container
    static final MinIOContainer MINIO = new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.enabled", () -> "true");
        registry.add("minio.endpoint",
                () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("minio.access-key", MINIO::getUserName);
        registry.add("minio.secret-key", MINIO::getPassword);
        registry.add("minio.bucket", () -> "it-sourcing-images");
        registry.add("minio.region", () -> "us-east-1");
    }

    @Autowired
    MinioStorageService minioStorageService;

    @Test
    void pingAndPutSmallObject() {
        assertThat(minioStorageService.ping()).isTrue();

        byte[] body = "minio-it".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        minioStorageService.putObject(null, "it/smoke.txt", body, "text/plain");

        assertThat(minioStorageService.presignGet(null, "it/smoke.txt", java.time.Duration.ofMinutes(5)))
                .isNotNull();
    }
}
