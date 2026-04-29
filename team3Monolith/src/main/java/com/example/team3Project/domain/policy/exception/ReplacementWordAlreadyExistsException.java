package com.example.team3Project.domain.policy.exception;

import java.io.Serial;

public class ReplacementWordAlreadyExistsException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = -7579398263992925466L;

    // 같은 사용자가 같은 sourceWord를 또 등록하려고 할 때 발생하는 예외이다.
    public ReplacementWordAlreadyExistsException(String message){
        super(message);
    }
}
