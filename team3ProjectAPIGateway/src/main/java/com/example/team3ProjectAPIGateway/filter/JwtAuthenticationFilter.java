package com.example.team3ProjectAPIGateway.filter;

import com.example.team3ProjectAPIGateway.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// 스프링에 빈으로 주입하여 싱글톤으로 관리함.
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private static final String VIA_GATEWAY_HEADER = "X-Via-Api-Gateway";
    private static final String VIA_GATEWAY_VALUE = "true";
    /** USER-SERVICE 로그인 시 발급하는 HttpOnly 쿠키 (브라우저 fetch + credentials 시 전달) */
    private static final String ACCESS_TOKEN_COOKIE = "token";

    /**
     * JWT 없이 통과시킬 경로 (게이트웨이는 막지 않음). 인증은 각 백엔드(세션/JWT)가 담당.
     * - /users/** : USER-SERVICE Thymeleaf·폼 (로그인 후 /users/update, /users/me 등) — 복수 users
     * - /sourcing/** GET·HEAD·OPTIONS : 폼 HTML만 (POST 는 X-User-Id 위해 게이트웨이에서 JWT 필요)
     * - /user/**  : 아래 목록 제외 시 게이트웨이에서 Bearer JWT 필수 — 단수 user (REST)
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/loginHome",
            "/user/gateway-check"
    );
    // JWTUtil -> 검증용 키를 만들고 파싱하는 역할을 함.
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // 모든 요청들은 여기로 들어옴. exchage = 모든 요청, 응답을 가지고 있음. chain = 다음 필터 또는 서비스로 이동함.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();
        if (isPublicPath(path, method)) {
            // 브라우저가 남긴 Bearer(만료/잘못된 토큰)이 그대로 넘어가면 USER-SERVICE OAuth2/Security 가 401 을 낼 수 있음
            ServerHttpRequest publicReq = exchange.getRequest().mutate()
                    .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                    .header(VIA_GATEWAY_HEADER, VIA_GATEWAY_VALUE)
                    .build();
            return chain.filter(exchange.mutate().request(publicReq).build());
        }

        String token = resolveAccessToken(exchange.getRequest());
        log.info("[JWT] path={}, method={}, tokenFound={}", path, method, token != null);
        if (token == null || token.isBlank()) {
            log.warn("[JWT] 토큰 없음 → 401: path={}", path);
            return unauthorized(exchange);
        }

        if (!jwtUtil.isValid(token)) {
            log.warn("[JWT] 토큰 검증 실패 → 401: path={}, token={}...{}", path,
                    token.substring(0, Math.min(20, token.length())),
                    token.length() > 20 ? token.substring(token.length() - 10) : "");
            return unauthorized(exchange);
        }

        // 유저 토큰 받아 저장.
        Claims claims = jwtUtil.parseClaims(token);
        // 유저 토큰을 하나씩 빼가며 유저 정보 가져오기.
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(VIA_GATEWAY_HEADER, VIA_GATEWAY_VALUE)
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Name", claims.get("username", String.class))
                .header("X-User-Nickname", claims.get("nickname", String.class))
                .build();
        // 유저 정보 가져온 요청을 다음 필터 또는 서비스로 이동함.
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    /** {@code Authorization: Bearer} 우선, 없으면 로그인 시 내려준 access_token 쿠키. */
    private String resolveAccessToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        HttpCookie c = cookies.getFirst(ACCESS_TOKEN_COOKIE);
        if (c == null) {
            return null;
        }
        String v = c.getValue();
        return v != null ? v.trim() : null;
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if ("/".equals(path)) {
            return true;
        }
        if (path.startsWith("/users/") || "/users".equals(path)) {
            return true;
        }
        if (path.startsWith("/sourcing/") && isSourcingReadOrCors(method)) {
            return true;
        }
        return PUBLIC_PATHS.stream().anyMatch(prefix -> path.startsWith(prefix));
    }

    private static boolean isSourcingReadOrCors(HttpMethod method) {
        return method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) { // 권한 없음. 401 응답 반환.
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // 401 응답 반환.
        return exchange.getResponse().setComplete(); // 응답 완료.
    }

    @Override // 여기부터 시작하게 함.
    public int getOrder() { // 필터 순서 설정.
        return -1; // 가장 먼저 실행.
    }
}
