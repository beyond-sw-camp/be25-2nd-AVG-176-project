package com.example.team3Project.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 로그인 요청을 위한 DTO
// getter, setter, toString, equals, hashCode 메서드가 자동 생성
@Data
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
