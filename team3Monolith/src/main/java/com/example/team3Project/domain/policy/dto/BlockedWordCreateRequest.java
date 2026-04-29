package com.example.team3Project.domain.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 클라이언트가 금지어를 추가할 때 보내는 요청값을 받는 DTO
@Getter
@NoArgsConstructor
public class BlockedWordCreateRequest {
    @NotBlank
    // 요청으로 들어온 값이 null, 빈 문자열, 공백만 있는 문자열도 안됨
    // @NotBlank => 글자가 있는 문자열이어야 한다.
    // 컨트롤러에서 @Valid가 붙어 있을 때 검증 실패가 발생한다.
    private String blockedWord;
}
