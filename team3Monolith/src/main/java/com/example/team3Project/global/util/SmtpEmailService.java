package com.example.team3Project.global.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[AVG Height] 비밀번호 재설정 인증코드");

            String htmlContent = buildVerificationCodeEmail(code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("인증코드 메일 발송 성공: to={}", to);
        } catch (MessagingException e) {
            log.error("인증코드 메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    public void sendTemporaryPassword(String to, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[AVG Height] 임시 비밀번호 안내");

            String htmlContent = buildTempPasswordEmail(tempPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("임시 비밀번호 메일 발송 성공: to={}", to);
        } catch (MessagingException e) {
            log.error("임시 비밀번호 메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }
    }

    private String buildVerificationCodeEmail(String code) {
        return """
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;">
                <h2 style="color: #333; text-align: center;">비밀번호 재설정 인증코드</h2>
                <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <p style="margin: 0 0 15px 0;">안녕하세요.</p>
                    <p style="margin: 0 0 15px 0;">비밀번호 재설정을 위한 인증코드입니다:</p>
                    <div style="background: #fff; padding: 15px; border-radius: 4px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 24px; font-weight: bold; color: #007bff; letter-spacing: 2px;">"""
                + code +
                    """
                    </span>
                    </div>
                    <p style="margin: 0; color: #666; font-size: 14px;">⏰ 유효시간: 5분</p>
                    <p style="margin: 10px 0 0 0; color: #dc3545; font-size: 13px;">※ 본인이 요청하지 않았다면 즉시 비밀번호를 변경해주세요.</p>
                </div>
            </div>
            """;
    }

    private String buildTempPasswordEmail(String tempPassword) {
        return """
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif;">
                <h2 style="color: #333; text-align: center;">임시 비밀번호 안내</h2>
                <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <p style="margin: 0 0 15px 0;">안녕하세요.</p>
                    <p style="margin: 0 0 15px 0;">요청하신 임시 비밀번호입니다:</p>
                    <div style="background: #fff; padding: 15px; border-radius: 4px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 20px; font-weight: bold; color: #28a745; letter-spacing: 2px;">"""
                + tempPassword +
                    """
                    </span>
                    </div>
                    <p style="margin: 0 0 10px 0; color: #dc3545; font-weight: bold;">⚠️ 보안 주의사항</p>
                    <ul style="margin: 0; padding-left: 20px; color: #666; font-size: 14px;">
                        <li>로그인 후 반드시 비밀번호를 변경해주세요.</li>
                        <li>임시 비밀번호는 타인에게 노출되지 않도록 주의하세요.</li>
                        <li>본인이 요청하지 않았다면 즉시 고객센터로 문의해주세요.</li>
                    </ul>
                </div>
            </div>
            """;
    }
}
