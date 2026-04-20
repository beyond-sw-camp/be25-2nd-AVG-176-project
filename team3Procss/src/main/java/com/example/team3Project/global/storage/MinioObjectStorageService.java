package com.example.team3Project.global.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioProperties minioProperties;

    @Override
    public String createSourcingImageUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            // DB에는 objectKey만 저장하고, 화면/등록에서 필요할 때만 임시 접근 URL을 만든다.
            return minioClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket().getSourcing())
                            .object(objectKey)
                            .expiry(minioProperties.getPresignedUrlExpireMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 이미지 접근 URL을 만들 수 없습니다. objectKey=" + objectKey, e);
        }
    }

    private MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
