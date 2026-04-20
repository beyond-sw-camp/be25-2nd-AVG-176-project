package com.example.team3Project.domain.product.processing.dto;

import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
// 소싱 상품 가공 응답 DTO
public class ProductProcessingResultResponse {
    private final boolean excluded; // 가공 시 제외 여부
    private final String exclusionReason; // 가공 시 제외 사유
    private final String processedProductName; // 가공된 상품명
    private final String processedBrand; // 가공된 브랜드
    private final BigDecimal originalPrice; // 원가
    private final String currency; // 원가 통화 코드
    private final BigDecimal exchangeRate; // 적용 환율
    private final BigDecimal costInKrw; // 환율 적용 원가
    private final BigDecimal salePrice; // 판매가
    private final BigDecimal shippingFee; // 배송비
    private final String registrationStatus; // 등록상태
}
