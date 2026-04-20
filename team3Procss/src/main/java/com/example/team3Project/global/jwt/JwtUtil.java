package com.example.team3Project.global.jwt;

import com.example.team3Project.domain.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Duration ACCESS_TOKEN_EXPIRE_DURATION = Duration.ofHours(2);

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + ACCESS_TOKEN_EXPIRE_DURATION.toMillis());

        return Jwts.builder()
                // Gateway가 X-User-Id 헤더로 내려줄 값이다.
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("nickname", user.getNickname())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }
}
