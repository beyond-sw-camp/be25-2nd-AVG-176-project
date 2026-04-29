package com.example.team3Project.global.config;

import com.example.team3Project.global.security.CustomAuthenticationFailureHandler;
import com.example.team3Project.global.security.CustomAuthenticationSuccessHandler;
import com.example.team3Project.global.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.gateway-url:http://localhost:8081}")
    private String gatewayUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/test",
                                "/api/users/**",
                                "/api/users/login", "/api/users/login/**",
                                "/api/users/signup", "/api/users/signup/**",
                                "/api/users/check-username", "/api/users/check-username/**",
                                "/api/users/find-id", "/api/users/find-id/**",
                                "/api/users/reset-pw", "/api/users/reset-pw/**",
                                "/", "/users/login", "/users/login/**", "/users/signup", "/users/signup/**", "/users/check-username",
                                "/users/find-id", "/users/reset-pw", "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/users/login")
                        .loginProcessingUrl("/users/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/users/logout")
                        .logoutSuccessUrl("/users/login")
                        .deleteCookies("token")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Gateway è«?Frontend äºŒì‡±????‰ìŠœ (API Gateway æ¹²ê³•ì»??´ÑŠâ€?
        // ?ëª? ??€???ë¼µ?ëªƒë’— è«›ì„ë±??Gateway?????¹ ?ë¬ë 
        configuration.setAllowedOrigins(Arrays.asList(
                gatewayUrl,
                frontendUrl,
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);  // JWT ?‘ì¢ê¶??ê¾©ë„š???ê¾ªë¹ ?ê¾©ë‹”
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) -> {
            String requestUri = request.getRequestURI();
            String accept = request.getHeader("Accept");
            String requestedWith = request.getHeader("X-Requested-With");
            boolean isApiRequest = requestUri.startsWith("/api/");
            boolean wantsJson = accept != null && accept.contains("application/json");
            boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);

            // API/XHR ?”ì²­?€ redirect ?€??401 JSON ?‘ë‹µ
            if (isApiRequest || isAjax || wantsJson || requestUri.equals("/users/me")) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"error\":\"ë¡œê·¸?¸ì´ ?„ìš”?©ë‹ˆ??\",\"code\":\"UNAUTHORIZED\"}");
            } else {
                // ?¼ë°˜ ?˜ì´ì§€ ?”ì²­?€ ë¡œê·¸???˜ì´ì§€ë¡??´ë™
                response.sendRedirect("/users/login?redirectURL=" + requestUri);
            }
        };
    }
}

