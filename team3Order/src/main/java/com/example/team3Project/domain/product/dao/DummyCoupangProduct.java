package com.example.team3Project.domain.product.dao;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// dummy_coupang_product 테이블 매핑 엔티티
@Entity
@Table(name = "dummy_coupang_product")
@Getter
public class DummyCoupangProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_coupang_product_id")
    private Long id;

    @Column(name = "product_name")
    private String productName;

    private String brand;

    // 판매가 (원화)
    @Column(name = "sale_price")
    private BigDecimal salePrice;

    // 정가 (원화)
    @Column(name = "original_price")
    private BigDecimal originalPrice;

    // 배송비
    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    // 대표 이미지 URL
    @Column(name = "main_image_url", length = 1000)
    private String mainImageUrl;

    private String currency;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_product_id")
    private String sourceProductId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "dummy_product_registration_id")
    private Long dummyProductRegistrationId;

    @Column(name = "margin_krw")
    private BigDecimal marginKrw;

    // 실제 주문 건수 (주문 서비스에서 증가 처리)
    @Column(name = "order_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int orderCount = 0;

    // 상품 이미지 목록 (sort_order 오름차순)
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<DummyCoupangProductImage> images = new ArrayList<>();

    // 상품 옵션 목록
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<DummyCoupangProductOption> options = new ArrayList<>();

    public void incrementOrderCount(int quantity) {
        this.orderCount += quantity;
    }
}
