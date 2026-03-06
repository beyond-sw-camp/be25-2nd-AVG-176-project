package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
//test.json 파일 보고 만든 DTO. 한번 크롤링후 어떻게 될지 json 파일 보고 수정할수도?
public class SourcingDTO {
    // json파일에서 source_url을 찾아서 데이터를 저장하게 함. 하지만 이름이 같다면 굳이 JsonProperty를 사용할 필요 없음.
    @JsonProperty("source_url")
    // 그렇다면 왜 사용하는가? camelCase때문에.
    private String sourceUrl;

    @JsonProperty("site_name")
    private String siteName;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("collected_at")
    private String collectedAt;

    @JsonProperty("data")
    private ProductData data;

    @Data
    public static class ProductData {
        private String title;

        @JsonProperty("original_price")
        private BigDecimal originalPrice;

        private String currency;
        private String brand;

        @JsonProperty("main_image")
        private String mainImageUrl;

        @JsonProperty("description_images")
        private List<String> descriptionImages;

        private List<Option> options;

        @JsonProperty("stock_status")
        private String stockStatus;
    }

    @Data
    public static class Option {
        private String name;
        private List<String> values;
    }

}
