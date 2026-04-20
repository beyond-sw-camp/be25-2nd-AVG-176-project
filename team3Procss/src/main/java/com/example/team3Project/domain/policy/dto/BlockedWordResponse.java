package com.example.team3Project.domain.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 서비스와 컨트롤러에서 결과를 반환하기 위한 응답 DTO이다.
@Getter
@AllArgsConstructor
public class BlockedWordResponse {
    private Long userBlockedWordId;     // 금지어 테이블 PK - 금지어 1건의 고유 번호
    private Long userId;                // 어느 사용자의 금지어인가
    private String blockedWord;         // 실제 금지어 문자열
}
/*
    AllArgsConstructor를 이용하면 다음과 같이 작성이 가능하다.
    new BlockedWordResponse(
        blockedWord.getUserBlockedWordId(),
        blockedWord.getUserId(),
        blockedWord.getBlockedWord()
    )
*/