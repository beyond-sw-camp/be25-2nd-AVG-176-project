package com.example.team3Project.support.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * MinIO(S3 호환) 접속 설정. {@code minio.enabled=false} 이면 관련 빈을 등록하지 않습니다.
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** true일 때만 S3Client·MinioStorageService 등록 */
    private boolean enabled = false;

    /** 예: http://127.0.0.1:9000 */
    private String endpoint = "http://127.0.0.1:9000";

    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";

    /** 기본 업로드 버킷 */
    private String bucket = "sourcing-images";

    /** MinIO는 리전을 거의 쓰지 않지만 SDK 요구로 더미 값 사용 */
    private String region = "us-east-1";
}
