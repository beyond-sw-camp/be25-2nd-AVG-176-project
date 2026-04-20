package com.example.team3Project.support.auth;

// 이거 로그인 서버가 실행하면 삭제하기.
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API Gateway가 JWT 검증 후 넣어 주는 {@code X-User-Id} 만 사용합니다.
 * local 프로필에서는 헤더 없이도 {@code auth.default-user-id} 값으로 대체합니다.
 */
@Component
public class RequestUserIdResolver {
    // 이거 로그인 서버가 실행하면 삭제하기.
    @Value("${auth.default-user-id:#{null}}")
    private Long defaultUserId;

    public Long resolveForApi(HttpServletRequest request) {
        // 이거 로그인 서버가 실행하면 주석 제거.
        // return parseLongHeader(request.getHeader("X-User-Id"));

        // 여기는 로그인 서버가 실행되면 주석하고 사용하기.
        Long fromHeader = parseLongHeader(request.getHeader("X-User-Id"));
        return fromHeader != null ? fromHeader : defaultUserId;
    }

    private static Long parseLongHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
