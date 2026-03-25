package com.example.team3Project.domain.user;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private int loginFailCount = 0;

    @Column(nullable = false)
    private boolean locked = false;

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
}
