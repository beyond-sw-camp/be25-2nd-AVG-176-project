package com.example.team3Project.domain.policy.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// 정책 - 국내마켓 코드
public enum MarketCode {
    NAVER_SMART_STORE("네이버 스마트 스토어"),
    COUPANG("쿠팡"),
    ELEVEN_STREET("11번가"),
    GMARKET("지마켓"),
    AUCTION("옥션");

    private final String description;
}
