package com.example.team3Project.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false)
    private int loginFailCount = 0;

    @Column(nullable = false)
    private boolean locked = false;

    // 소셜 로그인 관련 필드
    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "social_linked")
    @Builder.Default
    private boolean socialLinked = false;

    @Column(name = "profile_image")
    private String profileImage;

    public void increaseLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.locked = true;
        }
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }

    public void unlock() {
        this.locked = false;
        this.loginFailCount = 0;
    }

    // 소셜 로그인 여부 확인
    public boolean isSocialUser() {
        return this.provider != null && this.provider != AuthProvider.LOCAL;
    }

    // 소셜 로그인 제공자 Enum
    public enum AuthProvider {
        LOCAL, GOOGLE, KAKAO, NAVER
    }
}
