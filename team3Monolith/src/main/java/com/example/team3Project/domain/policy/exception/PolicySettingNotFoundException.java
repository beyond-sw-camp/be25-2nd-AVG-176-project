package com.example.team3Project.domain.policy.exception;

import java.io.Serial;

// 정책 설정 데이터가 없을 때 던질 예외 클래스
public class PolicySettingNotFoundException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = -7794820928412807708L;

    public PolicySettingNotFoundException(String message) {
        super(message);
    }
}
