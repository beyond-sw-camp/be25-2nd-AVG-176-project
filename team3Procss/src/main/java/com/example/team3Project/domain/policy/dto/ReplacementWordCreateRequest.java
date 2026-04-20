package com.example.team3Project.domain.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 치환어를 등록할 때 클라이언트가 보내는 JSON 요청값을 받을 DTO - 치환어 등록 요청값 구조
@Getter
@NoArgsConstructor  // JSON을 자바 객체로 바인딩할 떄에 먼저 객체를 생성하기 위해서 필요하다.
// record 같은 방식을 쓰면 no-args 없이도 DTO를 받을 수 있다.
public class ReplacementWordCreateRequest {

    @NotBlank
    private String sourceWord;  // 원본단어

    @NotBlank
    private String replacementWord; // 치환어
}
