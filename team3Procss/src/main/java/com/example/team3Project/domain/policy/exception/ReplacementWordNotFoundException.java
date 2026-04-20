package com.example.team3Project.domain.policy.exception;

import java.io.Serial;

// 삭제 대상 치환어가 존재하지 않을 때 발생하는 Exception 객체를 만들음
// RuntimeException은 예외 메시지를 저장하는 기능을 갖고 있음 ->
public class ReplacementWordNotFoundException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = -2085308800173819190L;

    public ReplacementWordNotFoundException(String message){
        super(message);
    }
}
