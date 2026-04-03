package com.example.team3Project.global.interceptor;

import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCheckInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.debug("JWT 인증 체크: {}", requestURI);

        // JWT 토큰 추출
        String token = jwtUtil.resolveToken(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            log.warn("JWT 인증 실패: {}", requestURI);
            response.sendRedirect("/users/login?redirectURL=" + requestURI);
            return false;
        }

        log.debug("JWT 인증 성공: {}", requestURI);
        return true;
    }
}
