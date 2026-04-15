package com.example.team3Project.global.security;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.domain.user.UserRepository;
import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://100.119.201.17:9000}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 인증된 사용자 정보 가져오기
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // DB에서 사용자 정보 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + username));

        // 로그인 실패 횟수 초기화
        user.resetLoginFailCount();

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getNickname()
        );

        // HttpOnly Cookie 설정
        ResponseCookie cookie = jwtUtil.createJwtCookie(token);
        response.addHeader("Set-Cookie", cookie.toString());

        // 리다이렉트 URL 가져오기 (기본값: 환경변수 기반 Frontend 주소)
        String redirectURL = request.getParameter("redirectURL");
        if (redirectURL == null || redirectURL.isEmpty() || redirectURL.contains("/users/login") || redirectURL.equals("/")) {
            redirectURL = frontendUrl;
        }

        log.info("일반 로그인 성공: userId={}, username={}, redirectURL={}",
                user.getId(), username, redirectURL);

        // AJAX 요청인 경우 JSON 응답
        if (isAjaxRequest(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true,\"username\":\"" + username + "\"}");
        } else {
            response.sendRedirect(redirectURL);
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith) ||
                "application/json".equals(request.getContentType()) ||
                request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
    }
}
