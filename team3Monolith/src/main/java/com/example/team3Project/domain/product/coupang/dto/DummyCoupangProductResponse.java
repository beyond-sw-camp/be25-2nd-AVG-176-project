package com.example.team3Project.domain.product.coupang.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DummyCoupangProductResponse(
        Long dummyCoupangProductId,
        Long registrationId,
        String sourcingProductId,
        String productName,
        String sourceMarket,
        BigDecimal salePrice,
        BigDecimal marginRate,
        LocalDateTime registeredAt,
        String status
) {
}
