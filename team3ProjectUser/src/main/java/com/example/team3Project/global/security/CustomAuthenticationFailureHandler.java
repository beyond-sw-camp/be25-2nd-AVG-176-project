package com.example.team3Project.global.security;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.domain.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UserRepository userRepository;

    @Value("${app.gateway-url:http://100.119.201.17:9000}")
    private String gatewayUrl;

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String errorMessage;

        log.warn("로그인 실패: username={}, error={}", username, exception.getMessage());

        if (exception.getMessage().contains("소셜 로그인")) {
            errorMessage = "social";
        } else if (exception.getMessage().contains("잠겼습니다")) {
            errorMessage = "locked";
        } else if (exception instanceof BadCredentialsException) {
            // 비밀번호 불일치 - 로그인 실패 횟수 증가
            errorMessage = "password";
            increaseLoginFailCount(username);
        } else {
            // 사용자를 찾을 수 없음
            errorMessage = "username";
        }

        // AJAX 요청인 경우 JSON 에러 응답
        if (isAjaxRequest(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"error\":\"" + errorMessage + "\"}");
        } else {
            // 리다이렉트 URL 유지 (Gateway 기준 절대경로)
            String redirectURL = request.getParameter("redirectURL");
            if (redirectURL == null || redirectURL.isEmpty() || redirectURL.equals("/")) {
                redirectURL = gatewayUrl;
            }

            String loginUrl = gatewayUrl + "/users/login?error=" + errorMessage + "&username=" +
                    (username != null ? java.net.URLEncoder.encode(username, "UTF-8") : "") +
                    "&redirectURL=" + java.net.URLEncoder.encode(redirectURL, "UTF-8");
            log.info("로그인 실패 후 리다이렉트: {}", loginUrl);
            response.sendRedirect(loginUrl);
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith) ||
                "application/json".equals(request.getContentType()) ||
                request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
    }

    private void increaseLoginFailCount(String username) {
        if (username == null || username.isEmpty()) return;

        userRepository.findByUsername(username).ifPresent(user -> {
            user.increaseLoginFailCount();
            log.warn("로그인 실패 횟수 증가: username={}, count={}, locked={}",
                    username, user.getLoginFailCount(), user.isLocked());
        });
    }
}
