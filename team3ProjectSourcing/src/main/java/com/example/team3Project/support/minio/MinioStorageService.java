package com.example.team3Project.support.minio;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * MinIO(S3 호환) 업로드·프리사인 URL 발급. 가공 서버는 키+자격 증명 또는 프리사인 GET으로 객체를 읽습니다.
 */
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioProperties properties;

    public String getDefaultBucket() {
        return properties.getBucket();
    }

    /**
     * 버킷이 없으면 생성합니다. MinIO 기동 전 호출 시 예외가 날 수 있으니 필요할 때만 호출하세요.
     */
    public void ensureBucketExists(String bucket) {
        String b = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(b).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(b).build());
            log.info("MinIO 버킷 생성: {}", b);
        }
    }

    public void putObject(String bucket, String objectKey, byte[] body, String contentType) {
        Objects.requireNonNull(objectKey, "objectKey");
        Objects.requireNonNull(body, "body");
        String b = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        ensureBucketExists(b);

        var reqBuilder = PutObjectRequest.builder().bucket(b).key(objectKey);
        if (StringUtils.hasText(contentType)) {
            reqBuilder.contentType(contentType);
        }
        s3Client.putObject(reqBuilder.build(), RequestBody.fromBytes(body));
    }

    public void putObject(String bucket, String objectKey, InputStream inputStream, long contentLength,
            String contentType) {
        Objects.requireNonNull(objectKey, "objectKey");
        Objects.requireNonNull(inputStream, "inputStream");
        String b = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        ensureBucketExists(b);

        var reqBuilder = PutObjectRequest.builder().bucket(b).key(objectKey);
        if (StringUtils.hasText(contentType)) {
            reqBuilder.contentType(contentType);
        }
        s3Client.putObject(reqBuilder.build(), RequestBody.fromInputStream(inputStream, contentLength));
    }

    /**
     * 가공 서버 등이 HTTP로 잠시 받을 수 있도록 GET 프리사인 URL을 만듭니다.
     */
    public URL presignGet(String bucket, String objectKey, Duration ttl) {
        String b = StringUtils.hasText(bucket) ? bucket : properties.getBucket();
        var get = GetObjectRequest.builder().bucket(b).key(objectKey).build();
        var presign = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        return s3Presigner.presignGetObject(presign).url();
    }

    /** 연결·버킷 존재 여부 확인 (헬스체크용) */
    public boolean ping() {
        try {
            ensureBucketExists(properties.getBucket());
            return true;
        } catch (S3Exception e) {
            log.warn("MinIO ping 실패: {}", e.getMessage());
            return false;
        }
    }
}
