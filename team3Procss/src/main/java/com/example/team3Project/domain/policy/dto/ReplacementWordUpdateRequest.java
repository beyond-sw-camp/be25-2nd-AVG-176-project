package com.example.team3Project.domain.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 치환어 수정 API에서 요청 Body를 받는 DTO
@Getter
@NoArgsConstructor
public class ReplacementWordUpdateRequest {

    @NotBlank
    // 해단 값에 공백이 오면 안된다.
    private String sourceWord;

    @NotBlank
    // 해단 값에 공백이 오면 안된다.
    private String replacementWord;
}