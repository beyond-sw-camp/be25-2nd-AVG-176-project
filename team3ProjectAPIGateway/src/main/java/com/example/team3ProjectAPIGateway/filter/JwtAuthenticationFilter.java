package com.example.team3ProjectAPIGateway.filter;

import com.example.team3ProjectAPIGateway.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

// 스프링에 빈으로 주입하여 싱글톤으로 관리함.
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private static final String VIA_GATEWAY_HEADER = "X-Via-Api-Gateway";
    private static final String VIA_GATEWAY_VALUE = "true";

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

        String authHeader = exchange.getRequest().getHeaders() // 요청 헤더를 가져옴.
                .getFirst(HttpHeaders.AUTHORIZATION); // AUTHORIZATION 헤더를 가져옴.
        // AUTHORIZATION 헤더가 없거나 Bearer 접두사가 없으면 권한 없음.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange); // 권한 없음. 401 응답 반환.
        }

        String token = authHeader.substring(7); // // "Bearer " 7글자 제거

        // 아까 만든 JwtUtil로 토큰이 유효한지 확인. 가짜거나 만료됐으면 401 반환하고 끝.
        if (!jwtUtil.isValid(token)) {
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
