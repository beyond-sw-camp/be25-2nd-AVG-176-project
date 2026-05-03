package com.example.team3Project.support.auth;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API Gateway가 JWT 검증 후 넣어 주는 {@code X-User-Id} 만 사용합니다.
 */
@Component
public class RequestUserIdResolver {

    public Long resolveForApi(HttpServletRequest request) {
        return parseLongHeader(request.getHeader("X-User-Id"));
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
