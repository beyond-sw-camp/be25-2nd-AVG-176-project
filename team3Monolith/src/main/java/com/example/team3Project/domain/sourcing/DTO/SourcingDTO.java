package com.example.team3Project.domain.sourcing.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SourcingDTO {
    // json의 데이터안에 있는 값들 매칭 시키기.
    @JsonProperty("url")
    private String url;

    @JsonProperty("asin")
    private String asin;

    @JsonProperty("title")
    private String title;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("url_image")
    private String urlImage;

    @JsonProperty("images")
    private List<String> images;

    @JsonProperty("variation")
    private List<VariationDTO> variation;

    @Data // 얘네의 경우 따로 묶여져서 다른 테이블에 넣음. 
    public static class VariationDTO {
        @JsonProperty("asin")
        private String asin;

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

        @JsonProperty("rating")
        private Double rating;

        @JsonProperty("reviews_count")
        private Integer reviewsCount;

        @JsonProperty("images")
        private List<String> images;
    }

}
