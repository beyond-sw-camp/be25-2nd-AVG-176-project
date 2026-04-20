package com.example.team3Project.domain.product.registration.application;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.product.coupang.application.DummyCoupangProductService;
import com.example.team3Project.domain.sourcing.SourcingCleanupRepository;
import com.example.team3Project.domain.product.processing.dto.SourcingVariationResponse;
import com.example.team3Project.domain.product.registration.dao.DummyProductRegistrationRepository;
import com.example.team3Project.domain.product.registration.dto.DummyProductRegistrationStatusCountsResponse;
import com.example.team3Project.domain.product.registration.entity.DummyProductImage;
import com.example.team3Project.domain.product.registration.entity.DummyProductImageType;
import com.example.team3Project.domain.product.registration.entity.DummyProductOption;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductRegistrationService {

    private final DummyProductRegistrationRepository dummyProductRegistrationRepository;
    private final DummyCoupangProductService dummyCoupangProductService;
    private final SourcingCleanupRepository sourcingCleanupRepository;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket.sourcing:sourcing-images}")
    private String sourcingImageBucket = "sourcing-images";

    public DummyProductRegistration register(
            Long userId,
            MarketCode marketCode,
            String sourceProductId,
            String sourceUrl,
            String mainImageUrl,
            List<String> descriptionImageUrls,
            List<SourcingVariationResponse> sourcingVariations,
            String processedProductName,
            String processedBrand,
            BigDecimal originalPrice,
            String currency,
            BigDecimal exchangeRate,
            PriceRoundingUnit roundingUnit,
            BigDecimal costInKrw,
            BigDecimal salePrice,
            BigDecimal shippingFee,
            RegistrationStatus registrationStatus,
            String exclusionReason
    ) {
        return register(
                userId,
                marketCode,
                sourceProductId,
                sourceUrl,
                mainImageUrl,
                descriptionImageUrls,
                sourcingVariations,
                processedProductName,
                processedBrand,
                originalPrice,
                currency,
                exchangeRate,
                roundingUnit,
                costInKrw,
                salePrice,
                BigDecimal.ZERO,
                shippingFee,
                registrationStatus,
                exclusionReason
        );
    }

    // 더미 등록 저장 시 같은 사용자/상품 조합은 새로 만들지 않고 기존 데이터를 갱신한다.
    public DummyProductRegistration register(
            Long userId,
            MarketCode marketCode,
            String sourceProductId,
            String sourceUrl,
            String mainImageUrl,
            List<String> descriptionImageUrls,
            List<SourcingVariationResponse> sourcingVariations,
            String processedProductName,
            String processedBrand,
            BigDecimal originalPrice,
            String currency,
            BigDecimal exchangeRate,
            PriceRoundingUnit roundingUnit,
            BigDecimal costInKrw,
            BigDecimal salePrice,
            BigDecimal marginKrw,
            BigDecimal shippingFee,
            RegistrationStatus registrationStatus,
            String exclusionReason
    ) {
        DummyProductRegistration registration = dummyProductRegistrationRepository
                .findByUserIdAndMarketCodeAndSourceProductId(userId, marketCode, sourceProductId)
                .orElseGet(() -> DummyProductRegistration.create(
                        userId,
                        marketCode,
                        sourceProductId,
                        sourceUrl,
                        mainImageUrl,
                        processedProductName,
                        processedBrand,
                        originalPrice,
                        currency,
                        exchangeRate,
                        costInKrw,
                        salePrice,
                        marginKrw,
                        shippingFee,
                        registrationStatus,
                        exclusionReason
                ));

        registration.update(
                sourceUrl,
                mainImageUrl,
                processedProductName,
                processedBrand,
                originalPrice,
                currency,
                exchangeRate,
                costInKrw,
                salePrice,
                marginKrw,
                shippingFee,
                registrationStatus,
                exclusionReason
        );
        registration.replaceOptions(toDummyOptions(sourcingVariations, exchangeRate, roundingUnit));
        registration.replaceImages(toDummyImages(mainImageUrl, descriptionImageUrls, sourcingVariations));

        return dummyProductRegistrationRepository.save(registration);
    }

    // 로그인 사용자와 마켓 코드 기준으로 등록 목록을 조회한다.
    public List<DummyProductRegistration> getRegistrations(Long userId, MarketCode marketCode) {
        return dummyProductRegistrationRepository.findByUserIdAndMarketCode(userId, marketCode);
    }

    @Transactional(readOnly = true)
    public List<DummyProductRegistration> searchReadyProducts(
            Long userId,
            LocalDate from,
            LocalDate to,
            String source,
            BigDecimal minMarginRate,
            BigDecimal maxMarginRate,
            String keyword
    ) {
        return dummyProductRegistrationRepository.searchReadyProducts(
                userId,
                toStartOfDay(from),
                toExclusiveEndOfDay(to),
                normalize(source),
                minMarginRate,
                maxMarginRate,
                normalize(keyword)
        );
    }

    // 취소된 등록 상품 목록을 검색 조건에 맞게 반환한다.
    @Transactional(readOnly = true)
    public List<DummyProductRegistration> searchCanceledProducts(
            Long userId,
            LocalDate from,
            LocalDate to,
            String source,
            BigDecimal minMarginRate,
            BigDecimal maxMarginRate,
            String keyword
    ) {
        return dummyProductRegistrationRepository.searchCanceledProducts(
                userId,
                toStartOfDay(from),
                toExclusiveEndOfDay(to),
                normalize(source),
                minMarginRate,
                maxMarginRate,
                normalize(keyword)
        );
    }

    @Transactional(readOnly = true)
    public List<DummyProductRegistration> searchRegisteredProducts(
            Long userId,
            LocalDate from,
            LocalDate to,
            String source,
            BigDecimal minMarginRate,
            BigDecimal maxMarginRate,
            String keyword
    ) {
        return dummyProductRegistrationRepository.searchRegisteredProducts(
                userId,
                toStartOfDay(from),
                toExclusiveEndOfDay(to),
                normalize(source),
                minMarginRate,
                maxMarginRate,
                normalize(keyword)
        );
    }

    // 등록 대기 화면에서 상태별 카드/뱃지에 바로 사용할 수 있도록 개수만 집계한다.
    @Transactional(readOnly = true)
    public DummyProductRegistrationStatusCountsResponse getStatusCounts(Long userId, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

        long readyCount = countByStatus(userId, RegistrationStatus.READY, fromDateTime, toDateTime);
        long registeredCount = countByStatus(userId, RegistrationStatus.REGISTERED, fromDateTime, toDateTime);
        long failedCount = countByStatus(userId, RegistrationStatus.FAILED, fromDateTime, toDateTime);
        long canceledCount = countByStatus(userId, RegistrationStatus.BLOCKED, fromDateTime, toDateTime);
        long failedOrCanceledCount = dummyProductRegistrationRepository.countByUserIdAndRegistrationStatusInAndReceivedAtRange(
                userId,
                List.of(RegistrationStatus.FAILED, RegistrationStatus.BLOCKED),
                fromDateTime,
                toDateTime
        );
        long totalCount = dummyProductRegistrationRepository.countByUserIdAndReceivedAtRange(
                userId,
                fromDateTime,
                toDateTime
        );

        return new DummyProductRegistrationStatusCountsResponse(
                readyCount,
                registeredCount,
                failedOrCanceledCount,
                totalCount,
                failedCount,
                canceledCount
        );
    }

    // 단건 조회는 현재 로그인 사용자의 소유 데이터만 반환한다.
    public DummyProductRegistration getRegistration(Long userId, Long registrationId) {
        return dummyProductRegistrationRepository.findByDummyProductRegistrationIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "등록 상품을 찾을 수 없습니다. id=" + registrationId
                ));
    }

    // 단건 삭제는 현재 로그인 사용자의 소유 데이터만 제거한다.
    public void deleteRegistration(Long userId, Long registrationId) {
        DummyProductRegistration registration = dummyProductRegistrationRepository
                .findByDummyProductRegistrationIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "삭제할 등록 상품을 찾을 수 없습니다. id=" + registrationId
                ));

        dummyCoupangProductService.deleteBySourceProductId(userId, registration.getSourceProductId());
        sourcingCleanupRepository.deleteByUserIdAndProductId(userId, registration.getSourceProductId());
        dummyProductRegistrationRepository.delete(registration);
    }

    // 선택 삭제는 전달받은 ID 목록이 모두 현재 로그인 사용자 소유일 때만 한 번에 제거한다.
    public void deleteRegistrations(Long userId, List<Long> registrationIds) {
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(registrationIds));
        List<DummyProductRegistration> registrations =
                dummyProductRegistrationRepository.findAllByDummyProductRegistrationIdInAndUserId(distinctIds, userId);

        if (registrations.size() != distinctIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "삭제할 등록 상품 중 조회되지 않는 항목이 있습니다."
            );
        }

        List<String> sourceProductIds = registrations.stream()
                .map(DummyProductRegistration::getSourceProductId)
                .toList();
        dummyCoupangProductService.deleteBySourceProductIds(userId, sourceProductIds);
        sourcingCleanupRepository.deleteByUserIdAndProductIds(userId, sourceProductIds);
        dummyProductRegistrationRepository.deleteAll(registrations);
    }

    public DummyProductRegistration updateRegistrationStatus(
            Long userId,
            Long registrationId,
            RegistrationStatus registrationStatus,
            String exclusionReason
    ) {
        DummyProductRegistration registration = getRegistration(userId, registrationId);

        if (registrationStatus == RegistrationStatus.REGISTERED) {
            dummyCoupangProductService.publishAutomatically(userId, registrationId);
            return registration;
        }

        if (registrationStatus == RegistrationStatus.BLOCKED) {
            dummyCoupangProductService.deleteBySourceProductId(userId, registration.getSourceProductId());
            String normalizedReason = normalize(exclusionReason);
            registration.markBlocked(normalizedReason != null ? normalizedReason : "SALES_STOPPED");
            return registration;
        }

        if (registrationStatus == RegistrationStatus.READY) {
            dummyCoupangProductService.deleteBySourceProductId(userId, registration.getSourceProductId());
            registration.markReady();
            return registration;
        }

        if (registrationStatus == RegistrationStatus.CANCELED) {
            // 쿠팡에 발행된 상품(REGISTERED)만 삭제한다. 발행 전(READY) 상품은 연결 레코드가 없으므로 건너뛴다.
            if (registration.getRegistrationStatus() == RegistrationStatus.REGISTERED) {
                dummyCoupangProductService.deleteBySourceProductId(userId, registration.getSourceProductId());
            }
            // sourcing 원본 테이블 레코드 정리 (sourcing, sourcing_variation 등)
            sourcingCleanupRepository.deleteByUserIdAndProductId(userId, registration.getSourceProductId());
            registration.markCanceled();
            return registration;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 등록 상태입니다. status=" + registrationStatus
        );
    }

    // variation 원본을 더미 등록용 옵션 엔티티 목록으로 변환한다.
    private List<DummyProductOption> toDummyOptions(
            List<SourcingVariationResponse> sourcingVariations,
            BigDecimal exchangeRate,
            PriceRoundingUnit roundingUnit
    ) {
        List<DummyProductOption> options = new ArrayList<>();
        if (sourcingVariations == null) {
            return options;
        }

        for (SourcingVariationResponse variation : sourcingVariations) {
            options.add(
                    DummyProductOption.create(
                            variation.getAsin(),
                            toDimensionsJson(variation.getDimensions()),
                            variation.isSelected(),
                            variation.getPrice(),
                            calculateOptionSalePrice(variation.getPrice(), exchangeRate, roundingUnit),
                            variation.getCurrency(),
                            variation.getStock(),
                            null,
                            null
                    )
            );
        }
        return options;
    }

    private BigDecimal calculateOptionSalePrice(
            BigDecimal originalPrice,
            BigDecimal exchangeRate,
            PriceRoundingUnit roundingUnit
    ) {
        if (originalPrice == null || exchangeRate == null) {
            return null;
        }
        BigDecimal salePrice = originalPrice.multiply(exchangeRate);
        if (roundingUnit == null) {
            return salePrice;
        }
        BigDecimal roundingAmount = BigDecimal.valueOf(roundingUnit.getAmount());
        return salePrice
                .divide(roundingAmount, 0, java.math.RoundingMode.UP)
                .multiply(roundingAmount);
    }

    // 대표/설명/옵션 이미지를 한 컬렉션으로 모아 등록 엔티티에 저장한다.
    private List<DummyProductImage> toDummyImages(
            String mainImageUrl,
            List<String> descriptionImageUrls,
            List<SourcingVariationResponse> sourcingVariations
    ) {
        List<DummyProductImage> images = new ArrayList<>();

        if (hasText(mainImageUrl)) {
            images.add(DummyProductImage.create(
                    DummyProductImageType.MAIN,
                    null,
                    sourcingImageBucket,
                    mainImageUrl,
                    imageSortOrder(mainImageUrl, 0)
            ));
        }

        if (descriptionImageUrls != null) {
            for (int i = 0; i < descriptionImageUrls.size(); i++) {
                String objectKey = descriptionImageUrls.get(i);
                if (!hasText(objectKey) || isMainDescriptionImage(objectKey, mainImageUrl)) {
                    continue;
                }
                images.add(
                        DummyProductImage.create(
                                DummyProductImageType.DESCRIPTION,
                                null,
                                sourcingImageBucket,
                                objectKey,
                                imageSortOrder(objectKey, i)
                        )
                );
            }
        }

        if (sourcingVariations != null) {
            for (SourcingVariationResponse variation : sourcingVariations) {
                List<String> optionImages = variation.getImages();
                if (optionImages == null) {
                    continue;
                }
                for (int i = 0; i < optionImages.size(); i++) {
                    String objectKey = optionImages.get(i);
                    if (!hasText(objectKey)) {
                        continue;
                    }
                    images.add(
                            DummyProductImage.create(
                                    DummyProductImageType.OPTION,
                                    variation.getAsin(),
                                    sourcingImageBucket,
                                    objectKey,
                                    imageSortOrder(objectKey, i)
                            )
                    );
                }
            }
        }

        return images;
    }

    private boolean isMainDescriptionImage(String objectKey, String mainImageObjectKey) {
        if (objectKey.equals(mainImageObjectKey)) {
            return true;
        }
        return objectKey.contains("/desc/") && imageSortOrder(objectKey, -1) == 0;
    }

    private Integer imageSortOrder(String objectKey, int fallback) {
        if (!hasText(objectKey)) {
            return fallback;
        }

        int slashIndex = objectKey.lastIndexOf('/');
        int dotIndex = objectKey.lastIndexOf('.');
        if (slashIndex < 0 || dotIndex <= slashIndex + 1) {
            return fallback;
        }

        try {
            return Integer.parseInt(objectKey.substring(slashIndex + 1, dotIndex));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long countByStatus(
            Long userId,
            RegistrationStatus status,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        return dummyProductRegistrationRepository.countByUserIdAndRegistrationStatusAndReceivedAtRange(
                userId,
                status,
                fromDateTime,
                toDateTime
        );
    }

    // 옵션 dimensions 맵은 JSON 문자열로 변환해 저장한다.
    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toExclusiveEndOfDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toDimensionsJson(Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("옵션 속성 정보를 문자열로 변환할 수 없습니다.", e);
        }
    }
}
