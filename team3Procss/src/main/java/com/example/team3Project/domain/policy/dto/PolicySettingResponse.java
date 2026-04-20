package com.example.team3Project.domain.policy.dto;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.policy.entity.ShippingFeeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
// 서버가 클라이언트에게 응답으로 돌려줄 데이터를 담는 객체이다.
public class PolicySettingResponse {
    private Long userPolicySettingId;
    private Long userId;
    private MarketCode marketCode;
    private BigDecimal targetMarginRate;
    private BigDecimal minMarginAmount;
    private BigDecimal marketFeeRate;
    private BigDecimal cardFeeRate;
    private BigDecimal exchangeRate;
    private PriceRoundingUnit roundingUnit;   // 단위 올림

    // AI 기반 자동 최적화
    private boolean amazonAutoPricingEnabled;     // amazon 가격 기준 판매가 자동 조정 사용 여부
    private boolean competitorAutoPricingEnabled; // 경쟁 상품 가격 자동 조정 사용 여부(화면 상 구현, 로직 구현 X)

    // 자동 보호 설정
    private boolean minMarginProtectEnabled;        // 최소 마진 보호
    private boolean priceAutoUpdateEnabled; // 환율 변동 시 가격 자동 업데이트
    private boolean stopLossEnabled;           // 손실 발생 시 판매 중지
    private boolean autoPublishEnabled;        // 해당 마켓 자동 발행 여부

    // 배송비 정책
    private ShippingFeeType shippingFeeType;       // 배송비 종류 (유료배송, 무료배송)
    private BigDecimal baseShippingFee;            // 기본 배송비
    private BigDecimal remoteAreaExtraShippingFee; // 제주 및 도서산간 추가비용
    private BigDecimal returnShippingFee;          // 반품비(편도)
}
