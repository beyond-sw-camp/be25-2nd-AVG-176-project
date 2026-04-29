package com.example.team3Project.domain.dummyMarket.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// 상품 상세 페이지 렌더링에 사용되는 DTO
@Getter
@Builder
public class ProductPageDto {

    private Long productId;
    private Long userId;
    private String productName;
    private String brand;
    private BigDecimal salePrice;
    private BigDecimal originalPrice;
    private BigDecimal shippingFee;

    // 대표 이미지 URL 목록 (갤러리 표시용)
    private List<String> mainImageUrls;

    // 상세 설명 이미지 URL 목록 (하단 상품 설명 영역)
    private List<String> descriptionImageUrls;

    // 옵션 목록
    private List<OptionDto> options;

    // 좌측 사이드바용 전체 상품 요약 목록
    private List<ProductSummaryDto> allProducts;
    private int sidebarPage;
    private int sidebarSize;
    private int sidebarTotalPages;
    private long sidebarTotalElements;
    private boolean sidebarHasPrevious;
    private boolean sidebarHasNext;

    // 옵션 정보 DTO
    @Getter
    @Builder
    public static class OptionDto {
        private Long optionId;
        private String optionAsin;
        private String optionDimensions; // 파싱된 옵션 속성 문자열
        private BigDecimal price;
        private boolean selected;        // 기본 선택 여부
        private String stock;
        private String currency;
        private String imageUrl;         // 옵션 대표 이미지 URL
    }

    // 사이드바 상품 목록에 사용되는 요약 DTO
    @Getter
    @Builder
    public static class ProductSummaryDto {
        private Long productId;
        private String productName;
        private String mainImageUrl;
        private BigDecimal salePrice;
    }
}
