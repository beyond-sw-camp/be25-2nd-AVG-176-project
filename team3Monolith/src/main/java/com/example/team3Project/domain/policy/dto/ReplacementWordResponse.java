package com.example.team3Project.domain.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor // 해당 DTO를 직접 만들어서 반환하기 위해서 필요하다. (내부적으로 객체 생성)
// 서버가 치환어 목록 조회 결과를 클라이언트에게 응답으로 내려줄 DTO
public class ReplacementWordResponse {
    private Long userReplacementWordId;

    private Long userId;

    private String sourceWord;

    private String replacementWord;
}
