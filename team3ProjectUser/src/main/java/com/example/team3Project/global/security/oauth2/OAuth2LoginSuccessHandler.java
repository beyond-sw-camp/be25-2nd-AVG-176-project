package com.example.team3Project.global.security.oauth2;

import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url:http://100.119.201.17:9000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공");

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(
                oauth2User.getId(),
                oauth2User.getUsername(),
                oauth2User.getNickname()
        );

        // HttpOnly Cookie 설정
        ResponseCookie cookie = jwtUtil.createJwtCookie(token);
        response.addHeader("Set-Cookie", cookie.toString());

        log.info("OAuth2 로그인 성공 - 사용자: {}, JWT 쿠키 발급 완료", oauth2User.getUsername());

        // 리다이렉트 URL 설정 (환경변수 기반 Frontend 주소)
        String targetUrl = frontendUrl;
        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }

        log.info("OAuth2 로그인 성공 후 리다이렉트: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
