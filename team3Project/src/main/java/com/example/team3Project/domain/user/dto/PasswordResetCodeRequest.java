package com.example.team3Project.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCodeRequest {

    @NotBlank(message = "아이디 또는 이메일을 입력해주세요.")
    private String loginIdOrEmail;
}
