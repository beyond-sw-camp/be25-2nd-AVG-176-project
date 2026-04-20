package com.example.team3Project.domain.product.processing.dto;

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
// 소싱 상품 가공 요청 DTO
public class ProductProcessingRequest {

    // 소싱의 ASIN(AmazonStandardIdentificationNumber)
    @NotBlank
    private String sourceProductId;

    // 소싱의 url
    @NotBlank
    private String sourceUrl;

    // 소싱의 translatedTitle
    @NotBlank
    private String translatedProductName;

    // 소싱의 translatedBrand
    @NotBlank
    private String translatedBrand;

    // 소싱의 price
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal originalPrice;

    // 소싱의 currency - 통화 코드(ex.USD)
    @NotBlank
    private String currency;

    // 소싱의 url_image
    @NotBlank
    // MinIO objectKey. 기존 필드명은 유지하지만 값은 이미지 URL이 아니라 objectKey다.
    private String mainImageUrl;

    // 소싱의 images
    @NotEmpty
    // MinIO objectKey 목록. desc 0번은 대표 이미지이므로 등록 저장 시 중복 저장을 피한다.
    private List<String> descriptionImageUrls;

    // 소싱 variation 원본을 함께 보관해 두고 등록 저장 단계에서 옵션/이미지 엔티티 생성에 사용한다.
    private List<SourcingVariationResponse> sourcingVariations;

    // 소싱 서비스 응답을 받아서 가공 흐름으로 넘길 때 사용할 객체
    // 정적 팩토리 메서드 - 클래스가 자기 자신을 만들어서 반환하는 static 메서드
    // 생성자 로직을 숨길 수 있음
    // 외부 소싱 payload를 내부 가공 서비스가 기대하는 입력 형식으로 맞출 때 사용한다.
    public static ProductProcessingRequest of(
            String sourceProductId,
            String sourceUrl,
            String translatedProductName,
            String translatedBrand,
            BigDecimal originalPrice,
            String currency,
            String mainImageUrl,
            List<String> descriptionImageUrls
    ) {
        return of(
                sourceProductId,
                sourceUrl,
                translatedProductName,
                translatedBrand,
                originalPrice,
                currency,
                mainImageUrl,
                descriptionImageUrls,
                List.of()
        );
    }

    public static ProductProcessingRequest of(
            String sourceProductId,
            String sourceUrl,
            String translatedProductName,
            String translatedBrand,
            BigDecimal originalPrice,
            String currency,
            String mainImageUrl,
            List<String> descriptionImageUrls,
            List<SourcingVariationResponse> sourcingVariations
    ) {
        ProductProcessingRequest request = new ProductProcessingRequest();
        request.sourceProductId = sourceProductId;
        request.sourceUrl = sourceUrl;
        request.translatedProductName = translatedProductName;
        request.translatedBrand = translatedBrand;
        request.originalPrice = originalPrice;
        request.currency = currency;
        request.mainImageUrl = mainImageUrl;
        request.descriptionImageUrls = descriptionImageUrls;
        request.sourcingVariations = sourcingVariations != null ? sourcingVariations : List.of();
        return request;
    }
}
