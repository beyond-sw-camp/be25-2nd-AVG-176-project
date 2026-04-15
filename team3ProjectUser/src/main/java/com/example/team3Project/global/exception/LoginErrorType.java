package com.example.team3Project.global.exception;

import lombok.Getter;

@Getter
public enum LoginErrorType {

    USERNAME_NOT_FOUND("존재하지 않는 아이디입니다."),
    PASSWORD_MISMATCH("비밀번호가 일치하지 않습니다."),
    ACCOUNT_LOCKED("로그인 실패 횟수 초과로 계정이 잠겼습니다. 관리자에게 문의하세요."),
    EMPTY_USERNAME("아이디를 입력해주세요."),
    EMPTY_PASSWORD("비밀번호를 입력해주세요."),
    USER_NOT_FOUND("등록된 사용자를 찾을 수 없습니다.");

    private final String message;

    LoginErrorType(String message) {
        this.message = message;
    }
}
