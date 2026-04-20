package com.example.team3Project.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // getter, setter,toString, equals, hashCode 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 자동 생성
public class PasswordResetRequest {

    @NotBlank(message = "아이디 또는 이메일을 입력해주세요.")
    private String loginIdOrEmail;

    @NotBlank(message = "인증코드를 입력해주세요.")
    private String code;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String newPassword;
}
