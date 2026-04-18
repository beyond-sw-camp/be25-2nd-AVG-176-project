package com.example.team3Project.domain.product.dao;

import jakarta.persistence.*;
import lombok.Getter;

// dummy_coupang_product_image 테이블 매핑 엔티티
@Entity
@Table(name = "dummy_coupang_product_image")
@Getter
public class DummyCoupangProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_coupang_product_image_id")
    private Long id;

    // 이미지 용도 구분: MAIN(대표), DESCRIPTION(상세설명), OPTION(옵션별)
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type")
    private ImageType imageType;

    // 외부 HTTP URL 또는 MinIO object key
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // 옵션 이미지일 경우 해당 옵션의 ASIN
    @Column(name = "option_asin")
    private String optionAsin;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "bucket_name")
    private String bucketName;

    // MinIO object key (http로 시작하지 않으면 MinIO 경로로 처리)
    @Column(name = "object_key", length = 1000)
    private String objectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_coupang_product_id")
    private DummyCoupangProduct product;

    // 이미지 타입 구분 enum
    public enum ImageType {
        MAIN, DESCRIPTION, OPTION
    }
}
