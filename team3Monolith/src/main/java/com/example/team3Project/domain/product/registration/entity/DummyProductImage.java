package com.example.team3Project.domain.product.registration.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dummy_product_image")
@Getter
@NoArgsConstructor
// 소싱에서 받은 이미지 정보를 등록 엔티티 아래에 분리 저장한다.
public class DummyProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_product_image_id")
    private Long dummyProductImageId;

    // JSON에 중복 작성되는 것을 막음
    @JsonBackReference("registration-images")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_product_registration_id", nullable = false)
    private DummyProductRegistration registration;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 30)
    // 해당 이미지가 메인이미지, 상세 설명이미지, 옵션 이미지 중 어느 것인지 구분한다.
    private DummyProductImageType imageType;

    @Column(name = "option_asin")
    // 옵션 이미지인 경우 어떤 옵션에 연결된 이미지인지 구분할 때 사용한다.
    private String optionAsin;

    // MinIO 버킷 이름이다. 현재 소싱 이미지는 sourcing-images 버킷을 사용한다.
    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    // MinIO objectKey다. 예: {userId}/{sourcingId}/desc/{ASIN}/0.jpeg
    @Column(name = "object_key", length = 1000)
    private String objectKey;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "sort_order")
    // 이미지 표시 순서
    private Integer sortOrder;

    // 더미 상품 이미지 객체 생성 메서드
    public static DummyProductImage create(
            DummyProductImageType imageType,
            String optionAsin,
            String imageUrl,
            Integer sortOrder
    ) {
        return create(imageType, optionAsin, null, imageUrl, sortOrder);
    }

    // MinIO objectKey를 기준으로 이미지 정보를 저장한다.
    public static DummyProductImage create(
            DummyProductImageType imageType,
            String optionAsin,
            String bucketName,
            String objectKey,
            Integer sortOrder
    ) {
        DummyProductImage image = new DummyProductImage();
        image.imageType = imageType;
        image.optionAsin = optionAsin;
        image.bucketName = bucketName;
        image.objectKey = objectKey;
        image.imageUrl = objectKey;
        image.sortOrder = sortOrder;
        return image;
    }

    // 현재 Image 객체가 어느 상품 등록 건에 소속되는지 맞춰준다.
    void assignRegistration(DummyProductRegistration registration) {
        this.registration = registration;
    }
}
