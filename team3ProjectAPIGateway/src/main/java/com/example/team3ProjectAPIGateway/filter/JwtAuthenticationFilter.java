package com.example.team3ProjectAPIGateway.filter;

import com.example.team3ProjectAPIGateway.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
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
    //여기는 검증 안해도 되는 경로.
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/signup",
            "/auth/find-id",
            "/auth/reset-pw"
    );
    // JWTUtil -> 검증용 키를 만들고 파싱하는 역할을 함.
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // 모든 요청들은 여기로 들어옴. exchage = 모든 요청, 응답을 가지고 있음. chain = 다음 필터 또는 서비스로 이동함.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath(); // 요청 경로를 가져옴.
        // 여기는 검증 안해도 되는 경로.
        if (isPublicPath(path)) {
            return chain.filter(exchange); // 다음 필터 또는 서비스로 이동함.
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
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Name", claims.get("username", String.class))
                .header("X-User-Nickname", claims.get("nickname", String.class))
                .build();
        // 유저 정보 가져온 요청을 다음 필터 또는 서비스로 이동함.
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    //공개 경로 체크 
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith); // 맞으면 True 반환. 아니면 False 반환.
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) { // 권한 없음. 401 응답 반환.
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // 401 응답 반환.
        return exchange.getResponse().setComplete(); // 응답 완료.
    }

    @Override
    public int getOrder() { // 필터 순서 설정.
        return -1; // 가장 먼저 실행.
    }
}
