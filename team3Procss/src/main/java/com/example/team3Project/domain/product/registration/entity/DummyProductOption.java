package com.example.team3Project.domain.product.registration.entity;

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
@Table(name = "dummy_product_option")
@Getter
@NoArgsConstructor
// 소싱 variation 한 건이 등록 저장 단계에서 옵션 엔티티 한 건으로 내려온다.
public class DummyProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_product_option_id")
    private Long dummyProductOptionId;

    // JSON 직렬화 시 부모 등록 엔티티로 다시 올라가며 순환 참조가 생기지 않게 막는다.
    @JsonBackReference("registration-options")
    // 옵션은 항상 특정 등록 상품 1건에 소속되므로 ManyToOne 관계로 둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_product_registration_id", nullable = false)
    private DummyProductRegistration registration;

    @Column(name = "option_asin", nullable = false)
    private String optionAsin;

    @Lob
    @Column(name = "option_dimensions")
    // variation 의 dimensions 값을 문자열 형태로 저장한다.
    private String optionDimensions;

    @Column(name = "selected", nullable = false)
    private boolean selected;

    // variation 에서 현재 실제 저장 대상으로 보는 가격 정보이다.
    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "currency", length = 10)
    private String currency;

    // variation 에서 현재 실제 저장 대상으로 보는 재고 상태이다.
    @Column(name = "stock", length = 1000)
    private String stock;

    // 평점과 리뷰 수는 필드는 열어 두되 현재 저장 로직에서는 제외 대상으로 정리했다.
    @Column(name = "rating")
    private Double rating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    // 더미 상품 옵션 엔티티 생성 메서드
    public static DummyProductOption create(
            String optionAsin,
            String optionDimensions,
            boolean selected,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            String currency,
            String stock,
            Double rating,
            Integer reviewsCount
    ) {
        DummyProductOption option = new DummyProductOption();
        option.optionAsin = optionAsin;
        option.optionDimensions = optionDimensions;
        option.selected = selected;
        option.originalPrice = originalPrice;
        option.salePrice = salePrice;
        option.currency = currency;
        option.stock = stock;
        option.rating = rating;
        option.reviewsCount = reviewsCount;
        return option;
    }

    // 현재 옵션 엔티티가 어느 등록 상품 소속인지 연관관계를 맞춘다.
    void assignRegistration(DummyProductRegistration registration) {
        this.registration = registration;
    }
}
