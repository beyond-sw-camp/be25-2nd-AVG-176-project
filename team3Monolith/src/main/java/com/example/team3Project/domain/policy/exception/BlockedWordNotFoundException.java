package com.example.team3Project.domain.policy.exception;

import java.io.Serial;

// 삭제하려는 금지어가 존재하지 않을 떄 던질 예외 클래스
public class BlockedWordNotFoundException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 7383314288747501084L;

    public BlockedWordNotFoundException(String message) {
        super(message);
    }
}
