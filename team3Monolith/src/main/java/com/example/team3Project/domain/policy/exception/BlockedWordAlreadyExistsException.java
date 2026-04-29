package com.example.team3Project.domain.policy.exception;

import java.io.Serial;

// extends Runtime exception - 실행 중 발생하는 오류
public class BlockedWordAlreadyExistsException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 7865808087693694291L;

    public BlockedWordAlreadyExistsException(String message){
        super(message);
    }
}
