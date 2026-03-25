package com.example.team3Project.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!email")
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("========================================");
        log.info("[이메일 발송 - 콘솔 모드]");
        log.info("수신자: {}", to);
        log.info("제목: 비밀번호 재설정 인증코드");
        log.info("내용: 인증코드는 [{}] 입니다. (유효시간: 5분)", code);
        log.info("========================================");
    }
}
