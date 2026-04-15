package com.example.team3Project.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class VerificationCodeStore {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 5;
    private static final int VERIFIED_EXPIRATION_MINUTES = 10;

    private final Map<String, VerificationCode> codeStore = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateAndStore(String email) {
        String code = generateCode();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        codeStore.put(email, new VerificationCode(code, expiration));
        log.info("인증코드 생성: email={}, expiration={}", email, expiration);
        return code;
    }

    public boolean verify(String email, String code) {
        VerificationCode stored = codeStore.get(email);
        if (stored == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(stored.expiration)) {
            codeStore.remove(email);
            return false;
        }
        return stored.code.equals(code);
    }

    public boolean verifyAndConsume(String email, String code) {
        VerificationCode stored = codeStore.remove(email);
        if (stored == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(stored.expiration)) {
            return false;
        }
        boolean valid = stored.code.equals(code);
        if (valid) {
            log.info("인증코드 검증 성공 및 소멸: email={}", email);
        }
        return valid;
    }

    public void remove(String email) {
        codeStore.remove(email);
    }

    public void markAsVerified(String email) {
        verifiedEmails.put(email, LocalDateTime.now().plusMinutes(VERIFIED_EXPIRATION_MINUTES));
        log.info("이메일 인증 완료 표시: email={}, 만료={}", email, 
                LocalDateTime.now().plusMinutes(VERIFIED_EXPIRATION_MINUTES));
    }

    public boolean isVerified(String email) {
        LocalDateTime expiration = verifiedEmails.get(email);
        if (expiration == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(expiration)) {
            verifiedEmails.remove(email);
            return false;
        }
        return true;
    }

    public void removeVerified(String email) {
        verifiedEmails.remove(email);
    }

    public void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        codeStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiration));
        verifiedEmails.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private record VerificationCode(String code, LocalDateTime expiration) {
    }
}
