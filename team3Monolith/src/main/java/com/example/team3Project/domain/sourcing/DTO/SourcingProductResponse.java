package com.example.team3Project.domain.sourcing.DTO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.entity.SourcingVariation;

import lombok.Builder;
import lombok.Value;

/**
 * 가공 등 타 서비스(Feign)로 노출하는 소싱 단건 JSON.
 */
@Value
@Builder
public class SourcingProductResponse {

    Long id;
    Long userId;
    /** DB product_id, 업로드 DTO의 asin과 동일 */
    String asin;
    SourcingRegistrationStatus registrationStatus;
    String sourceUrl;
    String siteName;
    String title;
    BigDecimal originalPrice;
    String currency;
    String brand;
    String mainImageUrl;
    List<String> descriptionImages;
    BigDecimal krwPrice;
    String translatedTitle;
    String translatedBrand;
    String resultImageUrl;
    List<VariationItem> variations;

    @Value
    @Builder
    public static class VariationItem {
        Long id;
        String asin;
        Map<String, String> dimensions;
        boolean selected;
        BigDecimal price;
        String currency;
        String stock;
        Double rating;
        Integer reviewsCount;
        List<String> images;
    }

    public static SourcingProductResponse fromEntity(Sourcing s) {
        List<SourcingVariation> vars = s.getVariations();
        List<VariationItem> items = vars == null ? List.of()
                : vars.stream().map(SourcingProductResponse::mapVariation).collect(Collectors.toList());

        return SourcingProductResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .asin(s.getProductId())
                .registrationStatus(s.getRegistrationStatus())
                .sourceUrl(s.getSourceUrl())
                .siteName(s.getSiteName())
                .title(s.getTitle())
                .originalPrice(s.getOriginalPrice())
                .currency(s.getCurrency())
                .brand(s.getBrand())
                .mainImageUrl(s.getMainImageUrl())
                .descriptionImages(s.getDescriptionImages() != null
                        ? List.copyOf(s.getDescriptionImages())
                        : List.of())
                .krwPrice(s.getKrwPrice())
                .translatedTitle(s.getTranslatedTitle())
                .translatedBrand(s.getTranslatedBrand())
                .resultImageUrl(s.getResultImageUrl())
                .variations(items)
                .build();
    }

    private static VariationItem mapVariation(SourcingVariation v) {
        Map<String, String> dims = v.getDimensions();
        List<String> imgs = v.getImages();
        return VariationItem.builder()
                .id(v.getId())
                .asin(v.getAsin())
                .dimensions(dims != null ? Map.copyOf(dims) : Collections.emptyMap())
                .selected(v.isSelected())
                .price(v.getPrice())
                .currency(v.getCurrency())
                .stock(v.getStock())
                .rating(v.getRating())
                .reviewsCount(v.getReviewsCount())
                .images(imgs != null ? List.copyOf(imgs) : List.of())
                .build();
    }
}
