package com.example.team3Project.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 예외가 발생했을 때 클라이언트에게 응답할 때 공통된 형식의 데이터를 담아주기 위한 DTO
public class ErrorResponse {
    private int status; // HTTP 상태 코드
    private String message; //에러 메시지
}
