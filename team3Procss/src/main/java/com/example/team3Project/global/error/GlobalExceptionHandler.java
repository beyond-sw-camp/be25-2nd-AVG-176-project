package com.example.team3Project.global.error;

import com.example.team3Project.domain.policy.exception.BlockedWordAlreadyExistsException;
import com.example.team3Project.domain.policy.exception.BlockedWordNotFoundException;
import com.example.team3Project.domain.policy.exception.PolicySettingNotFoundException;
import com.example.team3Project.domain.policy.exception.ReplacementWordAlreadyExistsException;
import com.example.team3Project.domain.policy.exception.ReplacementWordNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 전역 예외 처리 - Service에서 발생한 예외를 ResponseEntity에 담아 반환한다.
@RestControllerAdvice
public class GlobalExceptionHandler {
    /*// PolicySettingNotFoundException이 발생했을 때만 실행
    @ExceptionHandler(PolicySettingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlerPolicySettingNotFoundException(
            PolicySettingNotFoundException e) {
        // 클라이언트에게 응답할 JSON 데이터 만들기
        Map<String, Object> response = new HashMap<>();
        response.put("status", 404);
        response.put("message", e.getMessage());

        // 예외를 의도한 형태의 API 응답으로 바꿔서 반환
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }*/

    // PolicySettingNotFoundException이 발생했을 때만 실행
    @ExceptionHandler(PolicySettingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerPolicySettingNotFoundException(
            PolicySettingNotFoundException e
    ){
        // ErrorResponse dto에 내용을 담아 반환
        ErrorResponse response = new ErrorResponse(404, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /*// MethodArgumentNotValidException이 발생했을 때만 실행
    // - 들어와야 하는 범위 외의 값이 들어왔을 때
    // @Valid 검증 실패가 발생했을 때 실행된다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handlerMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        // 클라이언트에게 응답할 JSON 데이터 만들기
        Map<String, Object> response = new LinkedHashMap<>();
        // Http 상태코드 400에 응답 데이터를 넣는다.
        response.put("status", 400);
        response.put("message", "잘못된 요청값입니다.");

        // 에러의 상세사항 넣기 - 어느 필드가 왜 잘못되었는지
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult()
         .getFieldErrors()
         .forEach(error -> errors.put(error.getField(), error.getDefaultMessage())
         );

        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }*/

    // MethodArgumentNotValidException이 발생했을 때만 실행
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidaException(
            MethodArgumentNotValidException e
    ){
        // ErrorResponse dto에 내용을 담아 반환
        ErrorResponse response = new ErrorResponse(400, "잘못된 요청값입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // BlockedWordAlreadyExistsException(중복 금지어)이 발생했을 때 실행
    @ExceptionHandler(BlockedWordAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleBlockedWordAlreadyExistsException(
            BlockedWordAlreadyExistsException e
    ){
        // 409 : 충돌 상태 코드
        ErrorResponse response = new ErrorResponse(409, e.getMessage());
        // ResponseEntity에 HTTP 상태와 응답 바디를 담아 반환한다.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // BlockedWordNotFoundException이 발생했을 때 실행
    @ExceptionHandler(BlockedWordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerBlockedWordNotFoundException(
            BlockedWordNotFoundException e
    ){
        ErrorResponse response = new ErrorResponse(404, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ReplacementWordAlreadyExistsException이 발생했을 때
    @ExceptionHandler(ReplacementWordAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleReplacementWordAlreadyExistsException(
            ReplacementWordAlreadyExistsException e
    ){
        ErrorResponse response = new ErrorResponse(409, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ReplacementWordNotFoundException이 발생했을 때 실행
    @ExceptionHandler(ReplacementWordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerBlockedWordNotFoundException(
            ReplacementWordNotFoundException e
    ){
        ErrorResponse response = new ErrorResponse(404, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
