package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sourcing{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2048)
    private String sourceUrl;
    private String siteName;
    private String productId;

    // 여기는 data
    private String title;
    private BigDecimal originalPrice;
    private String currency;
    private String brand;
    private String mainImageUrl;

    // 이 부가 이미지때문에 따로 만든 테이블 얘네만 따로 모아 만듦.
    @ElementCollection // 이 어노테이션은 테이블을 따로 생성해줌.
    private List<String> descriptionImages;

    private BigDecimal krwPrice;
    private String translatedTitle;
    private String resultImageUrl;

    // 바로 정규화된 값들 setter로 지정.
    public void normalize(BigDecimal krwPrice, String translatedTitle, String mainImageUrl, String resultImageUrl) {
        this.krwPrice = krwPrice;
        this.translatedTitle = translatedTitle;
        this.mainImageUrl = mainImageUrl;
        this.resultImageUrl = resultImageUrl;
    }

    //상세 이미지 리스트 수정할수 있게 setter 지정
    public void setDescriptionImages(List<String> descriptionImages) {
        this.descriptionImages = descriptionImages;
    }

}