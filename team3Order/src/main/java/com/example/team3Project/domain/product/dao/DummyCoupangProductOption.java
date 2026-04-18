package com.example.team3Project.domain.product.dao;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

// dummy_coupang_product_option 테이블 매핑 엔티티
@Entity
@Table(name = "dummy_coupang_product_option")
@Getter
public class DummyCoupangProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_coupang_product_option_id")
    private Long id;

    private String currency;

    // 옵션 고유 식별자 (Amazon ASIN)
    @Column(name = "option_asin")
    private String optionAsin;

    // 옵션 속성 (JSON 문자열, 예: {"Color":"Red","Size":"M"})
    @Column(name = "option_dimensions", columnDefinition = "tinytext")
    private String optionDimensions;

    // 옵션 가격
    private BigDecimal price;

    // 기본 선택 여부
    private Boolean selected;

    // 재고 상태
    @Column(length = 1000)
    private String stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_coupang_product_id")
    private DummyCoupangProduct product;
}
