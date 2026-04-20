package com.example.team3Project.domain.policy.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// 정책 - 가격 올림 단위
public enum PriceRoundingUnit {
    TEN_THOUSAND_WON(10000, "만원 단위"),
    THOUSAND_WON(1000, "천원 단위"),
    HUNDRED_WON(100, "100원 단위"),
    FIFTY_WON(50, "50원 단위"),
    TEN_WON(10, "10원 단위");

    private final int amount;   // 계산용 숫자
    private final String description;  // 화면 표시
}
