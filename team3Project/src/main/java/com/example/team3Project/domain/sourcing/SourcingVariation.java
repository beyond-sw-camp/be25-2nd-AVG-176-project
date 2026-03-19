package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 여기는 상품 옵션 및 그 상품에 대한 정보.
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourcingVariation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 주요키
    private Long id;

    //일단 ManyToOne을 통해 1대다 형태를 나타냄. Lazy의 경우는 옵션 데이터만 일단 가져오고 부모데이터는 가져오지 않음.
    //가져오지 않아서 더 빠르게 가져올 수 있고 만약 부모 테이블이 가져와야 할 상황에야 비로소 가져옴.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sourcing_id")
    private Sourcing sourcing;

    private String asin;

    // 옵션 속성 (예: Flavor Name, Size) sourcing_variation_dimensions의 테이블을 따로 만듦
    @ElementCollection
    @CollectionTable(name = "sourcing_variation_dimensions", joinColumns = @JoinColumn(name = "variation_id"))
    @MapKeyColumn(name = "dimension_key")
    @Column(name = "dimension_value")
    private Map<String, String> dimensions;

    private boolean selected;
    private BigDecimal price;
    private String currency;

    //재고인데 current 뭐시기 저시기 라고 길게 나와서 일단 이거 2048에서 제거해야함.
    @Column(length = 2048)
    private String stock;
    private Double rating;
    private Integer reviewsCount;

    // 이미지 저장 엔티티
    @ElementCollection
    @CollectionTable(name = "sourcing_variation_images", joinColumns = @JoinColumn(name = "variation_id"))
    @Column(name = "image_url", length = 2048)
    private List<String> images;

    public void setImages(List<String> images) {
        this.images = images;
    }

}
