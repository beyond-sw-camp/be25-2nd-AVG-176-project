package com.example.team3Project.domain.product.coupang.application;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.product.coupang.dao.DummyCoupangProductRepository;
import com.example.team3Project.domain.product.coupang.dto.DummyCoupangProductResponse;
import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProduct;
import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProductImage;
import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProductOption;
import com.example.team3Project.domain.product.registration.dao.DummyProductRegistrationRepository;
import com.example.team3Project.domain.product.registration.entity.DummyProductImage;
import com.example.team3Project.domain.product.registration.entity.DummyProductOption;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DummyCoupangProductService {

    private final DummyCoupangProductRepository dummyCoupangProductRepository;
    private final DummyProductRegistrationRepository dummyProductRegistrationRepository;

    // 등록 후보 1건을 정책 기준 수동 발행 규칙에 따라 더미 쿠팡 상품으로 등록한다.
    public DummyCoupangProduct publishManually(Long userId, Long registrationId) {
        return publishRegistration(userId, registrationId);
    }

    // 가공 직후 자동 발행은 정책이 켜진 상태에서 내부 흐름만 사용한다.
    public DummyCoupangProduct publishAutomatically(Long userId, Long registrationId) {
        return publishRegistration(userId, registrationId);
    }

    private DummyCoupangProduct publishRegistration(Long userId, Long registrationId) {
        DummyProductRegistration registration = getPublishableRegistration(userId, registrationId);
        DummyCoupangProduct product = dummyCoupangProductRepository
                .findByUserIdAndSourceProductId(userId, registration.getSourceProductId())
                .orElseGet(() -> DummyCoupangProduct.create(registration));

        product.updateFromRegistration(registration);
        product.replaceOptions(toCoupangOptions(registration.getOptions()));
        product.replaceImages(toCoupangImages(registration.getImages()));
        registration.markRegistered();

        return dummyCoupangProductRepository.save(product);
    }

    // 여러 등록 후보를 한 번에 정책 기준 수동 발행 규칙에 따라 등록한다.
    public List<DummyCoupangProduct> publishAllManually(Long userId, List<Long> registrationIds) {
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(registrationIds));
        List<DummyCoupangProduct> products = new ArrayList<>();

        for (Long registrationId : distinctIds) {
            products.add(publishRegistration(userId, registrationId));
        }

        return products;
    }

    // 현재 로그인 사용자의 쿠팡 더미 상품 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<DummyCoupangProductResponse> getProducts(Long userId) {
        // 쿠팡 더미 테이블의 판매 중 상품을 프론트 테이블 응답 형식으로 변환한다.
        return dummyCoupangProductRepository.findSellingProducts(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DummyCoupangProductResponse> searchRegisteredProducts(
            Long userId,
            LocalDate from,
            LocalDate to,
            String source,
            BigDecimal minMarginRate,
            BigDecimal maxMarginRate,
            String keyword
    ) {
        return dummyCoupangProductRepository.searchRegisteredProducts(
                userId,
                toStartOfDay(from),
                toExclusiveEndOfDay(to),
                normalize(source),
                minMarginRate,
                maxMarginRate,
                normalize(keyword)
        ).stream()
                .map(this::toResponse)
                .toList();
    }

    // 현재 로그인 사용자의 쿠팡 더미 상품 상세를 조회한다.
    @Transactional(readOnly = true)
    public DummyCoupangProduct getProduct(Long userId, Long dummyCoupangProductId) {
        return dummyCoupangProductRepository.findByDummyCoupangProductIdAndUserId(dummyCoupangProductId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "상품을 찾을 수 없습니다. id=" + dummyCoupangProductId
                ));
    }

    public void deleteBySourceProductId(Long userId, String sourceProductId) {
        dummyCoupangProductRepository.deleteByUserIdAndSourceProductId(userId, sourceProductId);
    }

    public void deleteBySourceProductIds(Long userId, List<String> sourceProductIds) {
        if (sourceProductIds == null || sourceProductIds.isEmpty()) {
            return;
        }
        dummyCoupangProductRepository.deleteByUserIdAndSourceProductIdIn(userId, sourceProductIds);
    }

    // 마켓에 등록이 가능한 등록 후보인지 검증하고 사용자 소유 데이터만 가져온다.
    private DummyProductRegistration getPublishableRegistration(Long userId, Long registrationId) {
        DummyProductRegistration registration = dummyProductRegistrationRepository
                .findByDummyProductRegistrationIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "등록 상품을 찾을 수 없습니다. id=" + registrationId
                ));

        if (registration.getMarketCode() != MarketCode.COUPANG) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "쿠팡 마켓 등록 후보만 등록할 수 있습니다."
            );
        }

        if (registration.getRegistrationStatus() != RegistrationStatus.READY
                && registration.getRegistrationStatus() != RegistrationStatus.REGISTERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "등록 가능 상태의 상품만 쿠팡 더미 마켓에 발행할 수 있습니다."
            );
        }

        return registration;
    }

    // 등록 후보 옵션을 쿠팡 더미 상품 옵션으로 그대로 복사한다.
    private List<DummyCoupangProductOption> toCoupangOptions(List<DummyProductOption> options) {
        List<DummyCoupangProductOption> coupangOptions = new ArrayList<>();
        for (DummyProductOption option : options) {
            coupangOptions.add(
                    DummyCoupangProductOption.create(
                            option.getOptionAsin(),
                            option.getOptionDimensions(),
                            option.isSelected(),
                            option.getOriginalPrice(),
                            option.getSalePrice(),
                            option.getCurrency(),
                            option.getStock()
                    )
            );
        }
        return coupangOptions;
    }

    // 등록 후보 이미지를 쿠팡 더미 상품 이미지로 그대로 복사한다.
    private List<DummyCoupangProductImage> toCoupangImages(List<DummyProductImage> images) {
        List<DummyCoupangProductImage> coupangImages = new ArrayList<>();
        for (DummyProductImage image : images) {
            coupangImages.add(
                    DummyCoupangProductImage.create(
                            image.getImageType(),
                            image.getOptionAsin(),
                            image.getBucketName(),
                            image.getObjectKey(),
                            image.getSortOrder()
                    )
            );
        }
        return coupangImages;
    }

    private DummyCoupangProductResponse toResponse(DummyCoupangProduct product) {
        DummyProductRegistration registration = product.getRegistration();
        return new DummyCoupangProductResponse(
                product.getDummyCoupangProductId(),
                registration.getDummyProductRegistrationId(),
                product.getSourceProductId(),
                product.getProductName(),
                resolveSourceMarket(product.getSourceUrl()),
                product.getSalePrice(),
                calculateMarginRate(product),
                registration.getRegisteredAt(),
                "SELLING"
        );
    }

    private BigDecimal calculateMarginRate(DummyCoupangProduct product) {
        if (product.getMarginKrw() == null
                || product.getSalePrice() == null
                || BigDecimal.ZERO.compareTo(product.getSalePrice()) == 0) {
            return BigDecimal.ZERO;
        }
        return product.getMarginKrw()
                .multiply(BigDecimal.valueOf(100))
                .divide(product.getSalePrice(), 2, RoundingMode.HALF_UP);
    }

    private String resolveSourceMarket(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        try {
            String host = URI.create(sourceUrl).getHost();
            if (host == null || host.isBlank()) {
                return sourceUrl;
            }
            String normalizedHost = host.toLowerCase();
            if (normalizedHost.contains("amazon")) {
                return "AMAZON";
            }
            return normalizedHost;
        } catch (IllegalArgumentException e) {
            return sourceUrl;
        }
    }

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
}
