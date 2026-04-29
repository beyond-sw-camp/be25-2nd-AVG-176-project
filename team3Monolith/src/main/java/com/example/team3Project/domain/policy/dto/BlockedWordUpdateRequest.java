package com.example.team3Project.domain.policy.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 금지어 수정 API에서 요청 Body를 받는 DTO
@Getter
@NoArgsConstructor
public class BlockedWordUpdateRequest {
    @NotBlank   // null 불가, "", " " 불가
    private String blockedWord;
}
