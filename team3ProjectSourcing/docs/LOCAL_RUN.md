# 로컬 개발 기동 순서

1. **MariaDB** — 팀 공용 DB 접속 가능한지 확인 (`application-local.properties`의 JDBC URL).
2. **MinIO** — Spring과 별도 프로세스. API `9000`, 웹 콘솔 `9001`.
   - 예: `MINIO_ROOT_USER=minioadmin MINIO_ROOT_PASSWORD=minioadmin ~/minio server ~/minio-data --console-address ":9001"`
3. **Python (FastAPI)** — 이미지·텍스트 번역. `Python-Backend/image_translate.py` → 보통 `http://localhost:8000`.
4. **Spring (sourcing-service)** — `local` 프로필. MinIO 업로드는 `minio.enabled=true`일 때만 동작.

## 가공 서버에서 MinIO 이미지 사용

- **엔드포인트**: MinIO가 돌아가는 머신의 주소 + `:9000` (예: Tailscale IP).
- **버킷**: `sourcing-images`
- **자격 증명**: `minio.access-key` / `minio.secret-key`와 동일하게 설정 (또는 읽기 전용 계정).
- **객체 키**: DB·웹훅·`GET /sourcing/products/{asin}` 응답의 `descriptionImages`, `variations[].images` 문자열.
- Eureka에 MinIO를 등록할 필요 없음.

## 가공 웹훅

- `sourcing.processing.webhook-url` — 비우면 전송 안 함.
- 타임아웃·재시도: `sourcing.processing.webhook.connect-timeout-ms`, `read-timeout-ms`, `max-retries` (`application.properties` 참고).
