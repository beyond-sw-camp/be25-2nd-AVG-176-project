package com.example.team3Project.domain.sourcing.entity;

import java.math.BigDecimal;
import java.util.List;

import com.example.team3Project.domain.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

    /** 소싱을 등록한 회원 (동일 ASIN도 사용자별로 각각 보유 가능) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    @Builder.Default
    private SourcingRegistrationStatus registrationStatus = SourcingRegistrationStatus.PENDING_NORMALIZATION;
    
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

    @OneToMany(mappedBy = "sourcing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourcingVariation> variations;

    private BigDecimal krwPrice;
    private String translatedTitle;
    private String translatedBrand;
    private String resultImageUrl;

    // 바로 정규화된 값들 setter로 지정.
    public void normalize(BigDecimal krwPrice,String translatedTitle, String translatedBrand, String mainImageUrl, String resultImageUrl) {
        this.krwPrice = krwPrice;
        this.translatedTitle = translatedTitle;
        this.translatedBrand = translatedBrand;
        this.mainImageUrl = mainImageUrl;
        this.resultImageUrl = resultImageUrl;
    }

    //상세 이미지 리스트 수정할수 있게 setter 지정
    public void setDescriptionImages(List<String> descriptionImages) {
        this.descriptionImages = descriptionImages;
    }

    public void setRegistrationStatus(SourcingRegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }
    
}