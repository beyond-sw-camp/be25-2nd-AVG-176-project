package com.example.team3Project.domain.sourcing.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.team3Project.support.minio.MinioStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 번역된 결과 이미지(로컬 파일)를 MinIO에 업로드합니다.
 * Python 서버가 {@code /storage/resultImage/...} 경로로 돌려주는 파일을 읽어서 올립니다.
 * MinIO 비활성화 시 빈 미등록.
 */
@Service
@ConditionalOnBean(MinioStorageService.class)
@RequiredArgsConstructor
@Slf4j
public class SourcingMainImageMinioService {

    private static final Path RESOURCE_ROOT =
            Paths.get("src", "main", "resources").toAbsolutePath();

    private final MinioStorageService minioStorageService;

    /**
     * 번역된 결과 이미지를 MinIO에 업로드합니다.
     *
     * @param userId       사용자 PK
     * @param sourcingId   소싱 PK
     * @param resultImagePath Python 서버가 돌려준 경로 (예: {@code /storage/resultImage/result_xxx.png})
     * @return 업로드된 객체 키, 실패 시 empty
     */
    public Optional<String> uploadTranslatedImage(Long userId, Long sourcingId, String resultImagePath) {
        if (userId == null || sourcingId == null) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(resultImagePath)) {
            return Optional.empty();
        }

        Path localFile = resolveLocalFile(resultImagePath);
        if (localFile == null || !Files.exists(localFile)) {
            log.warn("번역 결과 이미지 파일 없음 sourcingId={} path={}", sourcingId, resultImagePath);
            return Optional.empty();
        }

        try {
            byte[] body = Files.readAllBytes(localFile);
            String contentType = detectContentType(localFile);
            String ext = extensionFromFilename(localFile.getFileName().toString());
            String objectKey = userId + "/" + sourcingId + "/translated_main." + ext;

            minioStorageService.putObject(null, objectKey, body, contentType);
            log.info("번역 결과 이미지 MinIO 업로드 완료 sourcingId={} key={}", sourcingId, objectKey);
            return Optional.of(objectKey);
        } catch (IOException ex) {
            log.warn("번역 결과 이미지 읽기 실패 sourcingId={} path={} err={}",
                    sourcingId, localFile, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("번역 결과 이미지 MinIO 업로드 실패 sourcingId={} err={}", sourcingId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 설명 이미지(description)나 variation 이미지의 번역 결과도 같은 방식으로 올립니다.
     *
     * @param suffix 키 구분 (예: {@code "desc/0"}, {@code "var/B0XXXXX/0"})
     */
    public Optional<String> uploadTranslatedImage(Long userId, Long sourcingId,
                                                   String resultImagePath, String suffix) {
        if (userId == null || sourcingId == null) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(resultImagePath)) {
            return Optional.empty();
        }

        Path localFile = resolveLocalFile(resultImagePath);
        if (localFile == null || !Files.exists(localFile)) {
            log.warn("번역 이미지 파일 없음 sourcingId={} path={}", sourcingId, resultImagePath);
            return Optional.empty();
        }

        try {
            byte[] body = Files.readAllBytes(localFile);
            String contentType = detectContentType(localFile);
            String ext = extensionFromFilename(localFile.getFileName().toString());
            String objectKey = userId + "/" + sourcingId + "/" + suffix + "." + ext;

            minioStorageService.putObject(null, objectKey, body, contentType);
            log.info("번역 이미지 MinIO 업로드 완료 sourcingId={} key={}", sourcingId, objectKey);
            return Optional.of(objectKey);
        } catch (IOException ex) {
            log.warn("번역 이미지 읽기 실패 sourcingId={} path={} err={}",
                    sourcingId, localFile, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("번역 이미지 MinIO 업로드 실패 sourcingId={} err={}", sourcingId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Python 서버가 돌려주는 상대 경로를 실제 로컬 파일로 변환합니다.
     * 예: {@code /storage/resultImage/result_xxx.png}
     *   → {@code src/main/resources/storage/resultImage/result_xxx.png}
     */
    private static Path resolveLocalFile(String resultImagePath) {
        String cleaned = resultImagePath.trim();
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return RESOURCE_ROOT.resolve(cleaned);
    }

    private static String detectContentType(Path file) {
        try {
            String ct = Files.probeContentType(file);
            if (ct != null) {
                return ct;
            }
        } catch (IOException ignored) {
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private static String extensionFromFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot > 0 && dot < lower.length() - 1) {
            String ext = lower.substring(dot + 1);
            if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg")
                    || ext.equals("webp") || ext.equals("gif")) {
                return ext;
            }
        }
        return "png";
    }
}
