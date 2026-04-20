package com.example.team3Project.domain.product.registration.entity;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "dummy_product_registration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dummy_registration_user_market_source",
                columnNames = {"user_id", "market_code", "source_product_id"}
        )
)
@Getter
@NoArgsConstructor
// 더미 상품 등록 엔티티
public class DummyProductRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_product_registration_id")
    private Long dummyProductRegistrationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_code", nullable = false)
    private MarketCode marketCode;

    @Column(name = "source_product_id", nullable = false)
    private String sourceProductId;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "main_image_url")
    private String mainImageUrl;

    @Column(name = "processed_product_name", nullable = false)
    private String processedProductName;

    @Column(name = "processed_brand", nullable = false)
    private String processedBrand;

    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "cost_in_krw", nullable = false)
    private BigDecimal costInKrw;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;

    // 판매가에서 매입원가와 예상 수수료를 뺀 원화 기준 마진이다.
    @Column(name = "margin_krw")
    private BigDecimal marginKrw;

    @Column(name = "shipping_fee", nullable = false)
    private BigDecimal shippingFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    private RegistrationStatus registrationStatus;

    @Column(name = "exclusion_reason")
    private String exclusionReason;

    // 소싱 ingest를 통해 등록 대기 테이블에 마지막으로 들어온 시점이다.
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    // 실제 마켓 등록이 완료된 시점이다. 아직 등록 전이면 null이다.
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    // 사용자가 등록을 취소한 시점이다. 취소 상태가 아니면 null이다.
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @JsonManagedReference("registration-options")
    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DummyProductOption> options = new ArrayList<>();

    @JsonManagedReference("registration-images")
    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DummyProductImage> images = new ArrayList<>();

    public static DummyProductRegistration create(
            Long userId,
            MarketCode marketCode,
            String sourceProductId,
            String sourceUrl,
            String mainImageUrl,
            String processedProductName,
            String processedBrand,
            BigDecimal originalPrice,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal costInKrw,
            BigDecimal salePrice,
            BigDecimal shippingFee,
            RegistrationStatus registrationStatus,
            String exclusionReason
    ) {
        return create(
                userId,
                marketCode,
                sourceProductId,
                sourceUrl,
                mainImageUrl,
                processedProductName,
                processedBrand,
                originalPrice,
                currency,
                exchangeRate,
                costInKrw,
                salePrice,
                BigDecimal.ZERO,
                shippingFee,
                registrationStatus,
                exclusionReason
        );
    }

    public static DummyProductRegistration create(
            Long userId,
            MarketCode marketCode,
            String sourceProductId,
            String sourceUrl,
            String mainImageUrl,
            String processedProductName,
            String processedBrand,
            BigDecimal originalPrice,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal costInKrw,
            BigDecimal salePrice,
            BigDecimal marginKrw,
            BigDecimal shippingFee,
            RegistrationStatus registrationStatus,
            String exclusionReason
    ) {
        DummyProductRegistration registration = new DummyProductRegistration();
        registration.userId = userId;
        registration.marketCode = marketCode;
        registration.sourceProductId = sourceProductId;
        registration.sourceUrl = sourceUrl;
        registration.mainImageUrl = mainImageUrl;
        registration.processedProductName = processedProductName;
        registration.processedBrand = processedBrand;
        registration.originalPrice = originalPrice;
        registration.currency = currency;
        registration.exchangeRate = exchangeRate;
        registration.costInKrw = costInKrw;
        registration.salePrice = salePrice;
        registration.marginKrw = marginKrw;
        registration.shippingFee = shippingFee;
        registration.registrationStatus = registrationStatus;
        registration.exclusionReason = exclusionReason;
        registration.receivedAt = LocalDateTime.now();
        registration.syncRegisteredAt(registrationStatus);
        return registration;
    }

    // 동일 상품이 다시 들어오면 기존 등록 건을 갱신한다.
    public void update(
            String sourceUrl,
            String mainImageUrl,
            String processedProductName,
            String processedBrand,
            BigDecimal originalPrice,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal costInKrw,
            BigDecimal salePrice,
            BigDecimal marginKrw,
            BigDecimal shippingFee,
            RegistrationStatus registrationStatus,
            String exclusionReason
    ) {
        this.sourceUrl = sourceUrl;
        this.mainImageUrl = mainImageUrl;
        this.processedProductName = processedProductName;
        this.processedBrand = processedBrand;
        this.originalPrice = originalPrice;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.costInKrw = costInKrw;
        this.salePrice = salePrice;
        this.marginKrw = marginKrw;
        this.shippingFee = shippingFee;
        this.registrationStatus = registrationStatus;
        this.exclusionReason = exclusionReason;
        this.receivedAt = LocalDateTime.now();
        syncRegisteredAt(registrationStatus);
        this.canceledAt = null; // ingest 재처리 시 취소 시각 초기화
    }

    public void replaceOptions(List<DummyProductOption> options) {
        this.options.clear();
        options.forEach(this::addOption);
    }

    public void replaceImages(List<DummyProductImage> images) {
        this.images.clear();
        images.forEach(this::addImage);
    }

    public void addOption(DummyProductOption option) {
        option.assignRegistration(this);
        this.options.add(option);
    }

    public void addImage(DummyProductImage image) {
        image.assignRegistration(this);
        this.images.add(image);
    }

    // 더미 마켓 발행이 끝난 등록 후보를 등록 완료 상태로 바꾼다.
    public void markRegistered() {
        this.registrationStatus = RegistrationStatus.REGISTERED;
        this.registeredAt = LocalDateTime.now();
        this.canceledAt = null;
    }

    public void markBlocked(String exclusionReason) {
        this.registrationStatus = RegistrationStatus.BLOCKED;
        this.exclusionReason = exclusionReason;
        this.registeredAt = null;
        this.canceledAt = null;
    }

    public void markReady() {
        this.registrationStatus = RegistrationStatus.READY;
        this.exclusionReason = null;
        this.registeredAt = null;
        this.canceledAt = null;
    }

    // 사용자가 등록을 취소할 때 호출한다. 사유와 등록 완료 시각은 초기화하고 취소 시각을 기록한다.
    public void markCanceled() {
        this.registrationStatus = RegistrationStatus.CANCELED;
        this.exclusionReason = null;
        this.registeredAt = null;
        this.canceledAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        syncRegisteredAt(registrationStatus);
    }

    private void syncRegisteredAt(RegistrationStatus status) {
        if (status == RegistrationStatus.REGISTERED && registeredAt == null) {
            registeredAt = LocalDateTime.now();
            return;
        }
        if (status != RegistrationStatus.REGISTERED) {
            registeredAt = null;
        }
    }
}
