package com.example.team3Project.global.exception;

import lombok.Getter;

@Getter
public class LoginException extends RuntimeException {

    private final LoginErrorType errorType;

    public LoginException(LoginErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }
}
