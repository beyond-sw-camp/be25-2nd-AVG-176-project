package com.example.team3Project.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // getter, setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 자동 생성
public class PasswordResetVerifyRequest {

    @NotBlank(message = "아이디 또는 이메일을 입력해주세요.")
    // 어떤 계정에 대한 인증인지 식별하기 위한 값
    private String loginIdOrEmail;

    @NotBlank(message = "인증코드를 입력해주세요.")
    // 사용자가 전달받은 인증코드
    private String code;
}
