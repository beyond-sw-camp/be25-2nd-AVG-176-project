package com.example.team3Project.domain.revenue.dto;

import java.math.BigDecimal;

// 도넛 차트용 예상/실제 마진 구성 합계 데이터
public record MarginSummaryResponse(
        MarginBreakdown expected,
        MarginBreakdown actual
) {

    // 각 도넛 차트 한 벌의 비용 구성 데이터
    public record MarginBreakdown(
            BigDecimal totalSalePrice,   // 총 판매가 합계
            BigDecimal productCost,      // 매입원가 합계
            BigDecimal platformFee,      // 플랫폼 수수료 합계 (판매가 - 원가 - 배송비 - 마진으로 역산)
            BigDecimal shippingFee,      // 배송비 합계
            BigDecimal margin,           // 마진 합계
            BigDecimal marginRate        // 마진율 (margin / totalSalePrice * 100)
    ) {}
}
