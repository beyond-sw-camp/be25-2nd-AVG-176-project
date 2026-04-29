package com.example.team3Project.domain.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity     // JPA가 관리하는 엔티티 클래스 - DB가 관리
// 이 클래스가 연결될 테이블 이름
@Table( name = "user_policy_setting",
        // 제약 조건 설정(사용자 ID, 마켓코드)쌍
        uniqueConstraints = {
                @UniqueConstraint (name = "uk_user_policy_setting_user_market",
                                    columnNames = {"user_id", "market_code"})
        })

@Getter
@NoArgsConstructor
public class UserPolicySetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)      // 자동 증가 값으로 생성
    @Column(name = "user_policy_setting_id")                 // DB 컬러명을 명시적으로 지정
    private Long userPolicySettingId;


    @Column(name = "user_id", nullable = false)
    private Long userId; // 사용자id

    // 기본 판매 가격 설정
    @Enumerated(EnumType.STRING)
    @Column(name = "market_code", nullable = false)
    private MarketCode marketCode; // 마켓 코드
    @Column(name = "target_margin_rate", nullable = false)
    private BigDecimal targetMarginRate;  // 목표 마진율
    @Column(name = "min_margin_amount", nullable = false)
    private BigDecimal minMarginAmount;   // 최소 마진(원)
    @Column(name = "market_fee_rate", nullable = false)
    private BigDecimal marketFeeRate;     // 마켓 수수료율(%)
    @Column(name = "card_fee_rate", nullable = false)
    private BigDecimal cardFeeRate;       // 카드 수수료(%)
    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;      // 환율(원)
    @Enumerated(EnumType.STRING)
    @Column(name = "price_rounding_unit", nullable = false)
    private PriceRoundingUnit roundingUnit;   // 단위 올림

    // AI 기반 자동 최적화
    @Column(name = "amazon_auto_pricing_enabled", nullable = false)
    private boolean amazonAutoPricingEnabled;     // amazon 가격 기준 판매가 자동 조정 사용 여부
    @Column(name = "competitor_auto_pricing_enabled", nullable = false)
    private boolean competitorAutoPricingEnabled; // 경쟁 상품 가격 자동 조정 사용 여부(화면 상 구현, 로직 구현 X)

    // 자동 보호 설정
    @Column(name = "min_margin_protect_enabled", nullable = false)
    private boolean minMarginProtectEnabled;        // 최소 마진 보호
    @Column(name = "price_auto_update_enabled",nullable = false)
    private boolean priceAutoUpdateEnabled; // 환율 변동 시 가격 자동 업데이트
    @Column(name = "stop_loss_enabled", nullable = false)
    private boolean stopLossEnabled;           // 손실 발생 시 판매 중지
    @Column(name = "auto_publish_enabled", nullable = false)
    private boolean autoPublishEnabled;        // 해당 마켓 자동 발행 여부 - 가공 후 등록 대기를 거치지 않고 자동 발행

    // 배송비 정책
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_fee_type", nullable = false)
    private ShippingFeeType shippingFeeType;       // 배송비 종류 (유료배송, 무료배송)
    @Column(name = "base_shipping_fee", nullable = false)
    private BigDecimal baseShippingFee;            // 기본 배송비
    @Column(name = "remote_area_extra_shipping_fee", nullable = false)
    private BigDecimal remoteAreaExtraShippingFee; // 제주 및 도서산간 추가비용
    // private BigDecimal freeShippingThreshold;   // 조건부 무료배송(시간 나면 구현)
    @Column(name = "return_shipping_fee", nullable = false)
    private BigDecimal returnShippingFee;          // 반품비(편도)



    // 새 정책 설정 엔티티를 만들 때 사용하는 정책 생성 메서드
    public static UserPolicySetting create(
            Long userId,
            MarketCode marketCode,
            BigDecimal targetMarginRate,
            BigDecimal minMarginAmount,
            BigDecimal marketFeeRate,
            BigDecimal cardFeeRate,
            BigDecimal exchangeRate,
            PriceRoundingUnit roundingUnit,
            boolean amazonAutoPricingEnabled,
            boolean competitorAutoPricingEnabled,
            boolean minMarginProtectEnabled,
            boolean priceAutoUpdateEnabled,
            boolean stopLossEnabled,
            boolean autoPublishEnabled,
            ShippingFeeType shippingFeeType,
            BigDecimal baseShippingFee,
            BigDecimal remoteAreaExtraShippingFee,
            BigDecimal returnShippingFee
    ) {
        UserPolicySetting setting = new UserPolicySetting();
        setting.userId = userId;
        setting.marketCode = marketCode;
        setting.targetMarginRate = targetMarginRate;
        setting.minMarginAmount = minMarginAmount;
        setting.marketFeeRate = marketFeeRate;
        setting.cardFeeRate = cardFeeRate;
        setting.exchangeRate = exchangeRate;
        setting.roundingUnit = roundingUnit;
        setting.amazonAutoPricingEnabled = amazonAutoPricingEnabled;
        setting.competitorAutoPricingEnabled = competitorAutoPricingEnabled;
        setting.minMarginProtectEnabled = minMarginProtectEnabled;
        setting.priceAutoUpdateEnabled = priceAutoUpdateEnabled;
        setting.stopLossEnabled = stopLossEnabled;
        setting.autoPublishEnabled = autoPublishEnabled;
        setting.shippingFeeType = shippingFeeType;
        setting.baseShippingFee = baseShippingFee;
        setting.remoteAreaExtraShippingFee = remoteAreaExtraShippingFee;
        setting.returnShippingFee = returnShippingFee;
        return setting;
    }

    // 기존 엔티티의 값을 수정
    public void update(
            BigDecimal targetMarginRate,
            BigDecimal minMarginAmount,
            BigDecimal marketFeeRate,
            BigDecimal cardFeeRate,
            BigDecimal exchangeRate,
            PriceRoundingUnit roundingUnit,
            boolean amazonAutoPricingEnabled,
            boolean competitorAutoPricingEnabled,
            boolean minMarginProtectEnabled,
            boolean priceAutoUpdateEnabled,
            boolean stopLossEnabled,
            boolean autoPublishEnabled,
            ShippingFeeType shippingFeeType,
            BigDecimal baseShippingFee,
            BigDecimal remoteAreaExtraShippingFee,
            BigDecimal returnShippingFee

    ) {
        this.targetMarginRate = targetMarginRate;
        this.minMarginAmount = minMarginAmount;
        this.marketFeeRate = marketFeeRate;
        this.cardFeeRate = cardFeeRate;
        this.exchangeRate = exchangeRate;
        this.roundingUnit = roundingUnit;
        this.amazonAutoPricingEnabled = amazonAutoPricingEnabled;
        this.competitorAutoPricingEnabled = competitorAutoPricingEnabled;
        this.minMarginProtectEnabled = minMarginProtectEnabled;
        this.priceAutoUpdateEnabled = priceAutoUpdateEnabled;
        this.stopLossEnabled = stopLossEnabled;
        this.autoPublishEnabled = autoPublishEnabled;
        this.shippingFeeType = shippingFeeType;
        this.baseShippingFee = baseShippingFee;
        this.remoteAreaExtraShippingFee = remoteAreaExtraShippingFee;
        this.returnShippingFee = returnShippingFee;
    }
}



