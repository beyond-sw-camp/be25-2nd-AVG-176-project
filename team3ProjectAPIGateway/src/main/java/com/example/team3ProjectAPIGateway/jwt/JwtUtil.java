package com.example.team3ProjectAPIGateway.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// 여기 역할은 쉽게 말해서 문자열이 우리가 아는 비밀키로 서명된 유효한 JWT인지를 확인하는 역할.
// 스프링에 빈으로 주입하여 싱글톤으로 관리함.
@Component
public class JwtUtil {
    // application.properties에 있는 JWT_SECRET을 가져옴. 토큰을 만든 쪽과 동일한 비밀키여야 검증이 됨.
    @Value("${jwt.secret}")
    private String secret;
    // 비밀키를 가져와서 서명을 만들어줌.
    private SecretKey signingKey;

    @PostConstruct // 서버가 시작할때 딱 한번 실행. secret를 이용한 검증용 키를 만듦.
    public void init() { 
        //문자열 secret을 JWT가 이해할 수 있는 키 객체로 변환하는 것 String을 사용해서는 JWT를 이해할수가 없음. 따라서 키 객체로 변환해줘야 함.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey) // 서명 확인
                .build()
                .parseSignedClaims(token) // 토큰을 파싱해서 페이로드를 가져옴.
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token); // 파싱 시도
            return true; // 유효하면 성공
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 유효하지 않으면 실패
        }
    }
}
