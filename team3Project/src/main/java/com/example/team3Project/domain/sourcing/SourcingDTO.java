package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SourcingDTO {
    
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


}
