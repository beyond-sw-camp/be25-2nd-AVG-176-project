package com.example.team3Project.global.security;

import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        log.debug("JWT filter start uri={}", requestUri);

        // JWT 토큰 추출 (Cookie 또는 Header)
        String token = jwtUtil.resolveToken(request);
        log.debug("resolved token is null? {}", token == null);

        if (token != null) {
            boolean isValid = jwtUtil.validateToken(token);
            log.debug("validateToken result={}", isValid);

            if (isValid) {
                // 토큰에서 username 추출
                String username = jwtUtil.getUsername(token);
                log.debug("username from token={}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // UserDetails 생성
                    UserDetails userDetails = new User(username, "", Collections.emptyList());

                    // Authentication 객체 생성 및 SecurityContext에 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("SecurityContext authentication set for username={}", username);
                    log.debug("JWT 인증 성공: username={}, uri={}", username, requestUri);
                } else {
                    log.debug("username is null or authentication already exists");
                }
            }
        } else {
            log.debug("token is null, skipping JWT authentication");
        }

        log.debug("JWT filter end uri={}", requestUri);
        filterChain.doFilter(request, response);
    }
}
