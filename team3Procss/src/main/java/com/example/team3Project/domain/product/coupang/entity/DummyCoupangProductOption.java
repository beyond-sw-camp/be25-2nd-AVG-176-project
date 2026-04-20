package com.example.team3Project.domain.product.coupang.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "dummy_coupang_product_option")
@Getter
@NoArgsConstructor
// 쿠팡 더미 상품의 옵션 정보를 저장한다.
public class DummyCoupangProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_coupang_product_option_id")
    private Long dummyCoupangProductOptionId;

    @JsonBackReference("coupang-product-options")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_coupang_product_id", nullable = false)
    private DummyCoupangProduct product;

    @Column(name = "option_asin", nullable = false)
    private String optionAsin;

    @Lob
    @Column(name = "option_dimensions")
    private String optionDimensions;

    @Column(name = "selected", nullable = false)
    private boolean selected;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "stock", length = 1000)
    private String stock;

    // 등록 후보 옵션을 쿠팡 더미 옵션으로 복사한다.
    public static DummyCoupangProductOption create(
            String optionAsin,
            String optionDimensions,
            boolean selected,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            String currency,
            String stock
    ) {
        DummyCoupangProductOption option = new DummyCoupangProductOption();
        option.optionAsin = optionAsin;
        option.optionDimensions = optionDimensions;
        option.selected = selected;
        option.originalPrice = originalPrice;
        option.salePrice = salePrice;
        option.currency = currency;
        option.stock = stock;
        return option;
    }

    void assignProduct(DummyCoupangProduct product) {
        this.product = product;
    }
}
