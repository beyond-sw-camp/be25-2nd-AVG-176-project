package com.example.team3Project.domain.product.processing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
// 소싱 요청 안의 상품 1건 아래에 있는 옵션 1건을 담는 DTO이다.
public class SourcingVariationResponse {
    @JsonProperty("asin")
    private String asin;

    // 옵션 속성 조합을 key-value 형태로 받는다.
    // 예) Flavor Name -> Coke Zero, Size -> 12 Fl Oz
    @JsonProperty("dimensions")
    private Map<String, String> dimensions;

    @JsonProperty("selected")
    private boolean selected;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("stock")
    private String stock;

    // 평점과 리뷰 수는 현재 variation 저장 범위에서 제외한다.
    @JsonProperty("rating")
    private Double rating;

    @JsonProperty("reviews_count")
    private Integer reviewsCount;

    // 옵션별 이미지 목록, DummyProductImage와 연결된다.
    @JsonProperty("images")
    // MinIO objectKey 목록이다. var 경로의 이미지는 옵션 이미지로 저장한다.
    private List<String> images;
}
