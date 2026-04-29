package com.example.team3Project.domain.dummyMarket.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductListPageDto {
    private List<ProductSummaryDto> products;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean hasPrevious;
    private boolean hasNext;

    @Getter
    @Builder
    public static class ProductSummaryDto {
        private Long productId;
        private Long userId;
        private String productName;
        private String mainImageUrl;
        private BigDecimal salePrice;
    }
}
