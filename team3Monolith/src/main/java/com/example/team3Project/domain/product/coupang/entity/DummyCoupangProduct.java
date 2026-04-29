package com.example.team3Project.domain.product.coupang.entity;

import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "dummy_coupang_product",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dummy_coupang_product_user_source",
                columnNames = {"user_id", "source_product_id"}
        )
)
@Getter
@NoArgsConstructor
// 쿠팡 더미 마켓에 등록된 상품 1건을 저장한다.
public class DummyCoupangProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_coupang_product_id")
    private Long dummyCoupangProductId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_product_id", nullable = false)
    private String sourceProductId;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "main_image_url", length = 1000)
    private String mainImageUrl;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;

    // 등록 데이터에서 계산된 원화 기준 예상 마진이다.
    @Column(name = "margin_krw")
    private BigDecimal marginKrw;

    @Column(name = "shipping_fee", nullable = false)
    private BigDecimal shippingFee;

    // 실제 주문 건수 (주문 서비스에서 증가 처리)
    @Column(name = "order_count", nullable = false)
    private int orderCount = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_product_registration_id", nullable = false)
    @JsonIgnore
    private DummyProductRegistration registration;

    @JsonManagedReference("coupang-product-options")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DummyCoupangProductOption> options = new ArrayList<>();

    @JsonManagedReference("coupang-product-images")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DummyCoupangProductImage> images = new ArrayList<>();

    // 등록 후보를 기반으로 쿠팡 더미 상품을 처음 만든다.
    public static DummyCoupangProduct create(DummyProductRegistration registration) {
        DummyCoupangProduct product = new DummyCoupangProduct();
        product.userId = registration.getUserId();
        product.sourceProductId = registration.getSourceProductId();
        product.registration = registration;
        product.updateFromRegistration(registration);
        return product;
    }

    // 같은 원본 상품을 다시 발행하면 쿠팡 더미 상품 내용을 갱신한다.
    public void updateFromRegistration(DummyProductRegistration registration) {
        this.sourceUrl = registration.getSourceUrl();
        this.mainImageUrl = registration.getMainImageUrl();
        this.productName = registration.getProcessedProductName();
        this.brand = registration.getProcessedBrand();
        this.originalPrice = registration.getOriginalPrice();
        this.currency = registration.getCurrency();
        this.salePrice = registration.getSalePrice();
        this.marginKrw = registration.getMarginKrw();
        this.shippingFee = registration.getShippingFee();
        this.registration = registration;
    }

    public void replaceOptions(List<DummyCoupangProductOption> options) {
        this.options.clear();
        options.forEach(this::addOption);
    }

    public void replaceImages(List<DummyCoupangProductImage> images) {
        this.images.clear();
        images.forEach(this::addImage);
    }

    public void addOption(DummyCoupangProductOption option) {
        option.assignProduct(this);
        this.options.add(option);
    }

    public void addImage(DummyCoupangProductImage image) {
        image.assignProduct(this);
        this.images.add(image);
    }
}
