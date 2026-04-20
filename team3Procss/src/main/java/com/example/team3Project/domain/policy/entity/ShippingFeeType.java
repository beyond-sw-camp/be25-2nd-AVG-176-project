package com.example.team3Project.domain.policy.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// 정책 - 배송비 유형
public enum ShippingFeeType {
    FREE_SHIPPING(0, "무료배송"),
    PAID_SHIPPING(1, "유료배송");

    private final int type;   // 배송비 유형
    private final String description;  // 화면 표시
}
