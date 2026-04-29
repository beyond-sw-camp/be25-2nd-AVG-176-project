package com.example.team3Project.domain.product.processing.dto;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// 소싱 서비스가 상품 수집과 번역을 마친 뒤 가공 서비스로 보내는 전달 요청 DTO다.
public class SourcingCompletedRequest {

    @NotNull
    @JsonProperty("marketCode")
    private MarketCode marketCode;

    @NotBlank
    @JsonProperty("asin")
    private String asin;

    @NotBlank
    @JsonProperty("brand")
    private String brand;

    @NotBlank
    @JsonProperty("currency")
    private String currency;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @JsonProperty("price")
    private BigDecimal price;

    @NotBlank
    @JsonProperty("title")
    private String title;

    @NotBlank
    @JsonProperty("url")
    private String url;

    // MinIO objectKey: {userId}/{sourcingId}/desc/{ASIN}/0.jpeg
    @NotBlank
    @JsonProperty("url_image")
    private String urlImage;

    // MinIO objectKey 목록이다. desc 0번은 대표 이미지, desc 1번 이후는 상세 이미지로 본다.
    @NotEmpty
    @JsonProperty("images")
    private List<String> images;

    // variation 데이터는 옵션별 가격, 재고, 이미지가 달라지므로 별도 목록으로 받는다.
    // 이후 등록 저장 단계에서 DummyProductOption, DummyProductImage 구조로 이어지는 원본 데이터다.
    @Valid
    @NotNull
    @JsonProperty("variation")
    private List<SourcingVariationResponse> variation;
}
