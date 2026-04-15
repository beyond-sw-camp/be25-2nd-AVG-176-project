-- 소셜 로그인 기능 추가를 위한 DB 스키마 변경 SQL

-- 기존 users 테이블에 소셜 로그인 관련 컬럼 추가
ALTER TABLE users 
ADD COLUMN provider VARCHAR(20) DEFAULT 'LOCAL' AFTER locked,
ADD COLUMN provider_id VARCHAR(255) AFTER provider,
ADD COLUMN social_linked TINYINT(1) DEFAULT 0 AFTER provider_id,
ADD COLUMN profile_image VARCHAR(500) AFTER social_linked;

-- provider_id 인덱스 추가 (소셜 로그인 조회 성능 향상)
CREATE INDEX idx_provider_id ON users(provider_id);

-- provider 컬럼 인덱스 추가
CREATE INDEX idx_provider ON users(provider);

-- 기존 데이터 마이그레이션: 모든 기존 사용자를 LOCAL로 설정
UPDATE users SET provider = 'LOCAL' WHERE provider IS NULL;

-- provider_id 유니크 제약조건 추가 (선택사항 - 같은 소셜 계정 중복 방지)
-- ALTER TABLE users ADD CONSTRAINT uk_provider_id UNIQUE (provider_id);

-- 변경 후 테이블 구조 확인
DESCRIBE users;
