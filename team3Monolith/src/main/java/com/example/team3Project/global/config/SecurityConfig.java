package com.example.team3Project.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 인증은 Gateway가 JWT로 처리하고 현재 서비스는 서버 세션에 인증 상태를 저장하지 않는다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Gateway 헤더 검증 필터를 붙이기 전까지는 기존 요청 흐름을 유지하기 위해 일단 모두 허용한다.
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                // Gateway 뒤의 내부 서비스이므로 HTTP Basic 인증은 사용하지 않는다.
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
