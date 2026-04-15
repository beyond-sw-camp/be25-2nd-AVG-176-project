package com.example.team3Project.global.interceptor;

import com.example.team3Project.global.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCheckInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.debug("JWT 인증 체크: {}", requestURI);

        // 인증 불필요 경로 바로 통과
        if (requestURI.startsWith("/users/login") ||
            requestURI.startsWith("/users/logout") ||
            requestURI.startsWith("/users/signup") ||
            requestURI.startsWith("/users/check-username") ||
            requestURI.startsWith("/users/find-id") ||
            requestURI.startsWith("/users/reset-pw") ||
            requestURI.startsWith("/api/users/find-id") ||
            requestURI.startsWith("/api/users/reset-pw") ||
            requestURI.startsWith("/oauth2") ||
            requestURI.startsWith("/login/oauth2") ||
            requestURI.startsWith("/css") ||
            requestURI.startsWith("/js") ||
            requestURI.startsWith("/images") ||
            requestURI.equals("/") ||
            requestURI.equals("/favicon.ico")) {
            return true;
        }

        // JWT 토큰 추출
        String token = jwtUtil.resolveToken(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            log.warn("JWT 인증 실패: {}", requestURI);

            // API 경로는 JSON 401 반환, 나머지는 로그인 페이지로 리다이렉트
            if (requestURI.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "error", "로그인이 필요합니다.",
                        "code", "UNAUTHORIZED"
                )));
            } else {
                response.sendRedirect("/users/login?redirectURL=" + requestURI);
            }
            return false;
        }

        log.debug("JWT 인증 성공: {}", requestURI);
        return true;
    }
}
