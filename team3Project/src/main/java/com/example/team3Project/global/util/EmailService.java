package com.example.team3Project.global.util;

public interface EmailService {
    void sendVerificationCode(String to, String code);
}
