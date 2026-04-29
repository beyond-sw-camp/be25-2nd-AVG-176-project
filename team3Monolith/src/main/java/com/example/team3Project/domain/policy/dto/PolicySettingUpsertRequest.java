package com.example.team3Project.domain.policy.dto;

import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.policy.entity.ShippingFeeType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// JSON 데이터를 자바 객체로 받기 위한 파일
// 정책 설정 저장,수정 요청 데이터를 담는 DTO
@Getter
@NoArgsConstructor
public class PolicySettingUpsertRequest {
    @NotNull
    @DecimalMin(value = "1.0") @DecimalMax(value = "100.0")
    private BigDecimal targetMarginRate; // 목표 마진율

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal minMarginAmount; // 최소 마진(원)

    @NotNull
    @DecimalMin(value = "1.0") @DecimalMax(value = "100.0")
    private BigDecimal marketFeeRate;   // 마켓 수수료율(%)

    @NotNull
    @DecimalMin(value = "1.0") @DecimalMax(value = "100.0")
    private BigDecimal cardFeeRate;    // 카드 수수료(%)

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal exchangeRate;   // 환율(원)

    @NotNull
    private PriceRoundingUnit roundingUnit;   // 단위 올림

    private boolean amazonAutoPricingEnabled;     // amazon 가격 기준 판매가 자동 조정 사용 여부

    private boolean competitorAutoPricingEnabled; // 경쟁 상품 가격 자동 조정 사용 여부

    private boolean minMarginProtectEnabled;    // 최소 마진 보호

    private boolean priceAutoUpdateEnabled; // 환율 변동 시 가격 자동 업데이트

    private boolean stopLossEnabled;           // 손실 발생 시 판매 중지

    private boolean autoPublishEnabled;        // 해당 마켓 자동 발행 여부

    @NotNull
    private ShippingFeeType shippingFeeType;       // 배송비 종류

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal baseShippingFee;            // 기본 배송비

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal remoteAreaExtraShippingFee; // 제주 및 도서산간 추가비용

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal returnShippingFee; // 반품비(편도)
}
