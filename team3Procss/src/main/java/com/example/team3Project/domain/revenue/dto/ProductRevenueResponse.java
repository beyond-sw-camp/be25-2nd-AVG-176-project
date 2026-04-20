package com.example.team3Project.domain.revenue.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 마진 페이지 상품별 테이블 한 행 데이터
public record ProductRevenueResponse(
        Long coupangProductId,
        String productName,
        BigDecimal salePrice,
        BigDecimal marginPerUnit,   // 상품 1개당 마진 (margin_krw)
        BigDecimal marginRate,      // 마진율 (%)
        LocalDateTime registeredAt,
        int orderCount,             // 실제 주문 건수
        int reviewCount             // 소싱 테이블 리뷰 수 합산
) {}
