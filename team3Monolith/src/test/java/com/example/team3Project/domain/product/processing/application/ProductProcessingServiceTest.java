package com.example.team3Project.domain.product.processing.application;

import com.example.team3Project.domain.policy.application.PolicyQueryService;
import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.policy.dto.PolicyBundle;
import com.example.team3Project.domain.policy.dto.PolicySettingResponse;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.policy.entity.ShippingFeeType;
import com.example.team3Project.domain.product.coupang.application.DummyCoupangProductService;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingRequest;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingResultResponse;
import com.example.team3Project.domain.product.processing.dto.SourcingVariationResponse;
import com.example.team3Project.domain.product.registration.application.ProductRegistrationService;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductProcessingServiceTest {

    @Test
    @DisplayName("금지어가 포함된 상품명은 기존 상품명 가공 흐름에서 제외된다")
    void processProductName_returnsEmpty_whenBlockedWordExists() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                defaultPolicySetting(),
                List.of(new BlockedWordResponse(1L, 1L, "무료배송")),
                List.of()
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);

        Optional<String> result =
                productProcessingService.processProductName(1L, MarketCode.COUPANG, "무료배송 이동식 선반");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("치환어가 있으면 기존 상품명 가공 흐름에서 치환어가 적용된다")
    void processProductName_appliesReplacementWords() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                defaultPolicySetting(),
                List.of(),
                List.of(
                        new ReplacementWordResponse(1L, 1L, "빠른배송", "당일출고"),
                        new ReplacementWordResponse(2L, 1L, "무료배송", "배송비포함")
                )
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);

        Optional<String> result =
                productProcessingService.processProductName(1L, MarketCode.COUPANG, "무료배송 빠른배송 이동식 선반");

        assertTrue(result.isPresent());
        assertEquals("배송비포함 당일출고 이동식 선반", result.get());
    }

    @Test
    @DisplayName("금지어도 치환어도 없으면 기존 상품명 가공 흐름은 원본을 그대로 반환한다")
    void processProductName_returnsOriginalName_whenNoPolicyMatches() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                defaultPolicySetting(),
                List.of(),
                List.of()
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);

        Optional<String> result =
                productProcessingService.processProductName(1L, MarketCode.COUPANG, "기본 이동식 선반");

        assertTrue(result.isPresent());
        assertEquals("기본 이동식 선반", result.get());
    }

    @Test
    @DisplayName("금지어가 포함되면 차단 상태로 저장하고 응답에서는 가격 정보를 비운다")
    void processProduct_returnsBlocked_whenBlockedWordExists() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                defaultPolicySetting(),
                List.of(new BlockedWordResponse(1L, 1L, "금지어")),
                List.of()
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);
        when(productRegistrationService.register(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any(List.class),
                any(List.class),
                anyString(),
                anyString(),
                any(BigDecimal.class),
                anyString(),
                any(BigDecimal.class),
                any(PriceRoundingUnit.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(),
                any()
        )).thenReturn(createRegistration(RegistrationStatus.BLOCKED, "BLOCKED_WORD"));

        ProductProcessingRequest request = createRequest("금지어 포함 상품", "브랜드", BigDecimal.valueOf(10));

        ProductProcessingResultResponse result =
                productProcessingService.processProduct(1L, MarketCode.COUPANG, request);

        ArgumentCaptor<RegistrationStatus> statusCaptor = ArgumentCaptor.forClass(RegistrationStatus.class);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);

        verify(productRegistrationService).register(
                eq(1L),
                eq(MarketCode.COUPANG),
                eq("ASIN-001"),
                eq("https://www.amazon.com/dp/ASIN-001"),
                eq("https://image.example/main.jpg"),
                eq(List.of("https://image.example/detail-1.jpg")),
                eq(List.of()),
                eq("금지어 포함 상품"),
                eq("브랜드"),
                eq(BigDecimal.valueOf(10)),
                eq("USD"),
                eq(BigDecimal.valueOf(1350)),
                eq(PriceRoundingUnit.HUNDRED_WON),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.ZERO),
                statusCaptor.capture(),
                reasonCaptor.capture()
        );

        assertEquals(RegistrationStatus.BLOCKED, statusCaptor.getValue());
        assertEquals("BLOCKED_WORD", reasonCaptor.getValue());

        assertTrue(result.isExcluded());
        assertEquals("BLOCKED_WORD", result.getExclusionReason());
        assertEquals("BLOCKED", result.getRegistrationStatus());
        assertNull(result.getCostInKrw());
        assertNull(result.getSalePrice());
        assertNull(result.getShippingFee());
    }

    @Test
    @DisplayName("최소 마진 보호가 꺼져 있으면 목표 마진율 기준으로 판매가를 계산하고 저장한다")
    void processProduct_usesMarginRateOnly_whenMinMarginProtectDisabled() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                policySettingWithMinMarginProtect(false, BigDecimal.valueOf(30), BigDecimal.valueOf(5000)),
                List.of(),
                List.of()
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);
        when(productRegistrationService.register(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any(List.class),
                any(List.class),
                anyString(),
                anyString(),
                any(BigDecimal.class),
                anyString(),
                any(BigDecimal.class),
                any(PriceRoundingUnit.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(),
                any()
        )).thenReturn(createRegistration(RegistrationStatus.READY, null));

        ProductProcessingRequest request = createRequest("정상 상품", "브랜드", BigDecimal.valueOf(10));

        ProductProcessingResultResponse result =
                productProcessingService.processProduct(1L, MarketCode.COUPANG, request);

        assertEquals(BigDecimal.valueOf(13500), result.getCostInKrw());
        assertEquals(BigDecimal.valueOf(20300), result.getSalePrice());
        assertEquals("READY", result.getRegistrationStatus());

        verify(productRegistrationService).register(
                eq(1L),
                eq(MarketCode.COUPANG),
                eq("ASIN-001"),
                eq("https://www.amazon.com/dp/ASIN-001"),
                eq("https://image.example/main.jpg"),
                eq(List.of("https://image.example/detail-1.jpg")),
                eq(List.of()),
                eq("정상 상품"),
                eq("브랜드"),
                eq(BigDecimal.valueOf(10)),
                eq("USD"),
                eq(BigDecimal.valueOf(1350)),
                eq(PriceRoundingUnit.HUNDRED_WON),
                eq(BigDecimal.valueOf(13500)),
                eq(BigDecimal.valueOf(20300)),
                eq(BigDecimal.valueOf(4100)),
                eq(BigDecimal.valueOf(3000)),
                eq(RegistrationStatus.READY),
                eq(null)
        );
    }

    @Test
    @DisplayName("variation 정보가 있으면 등록 저장 단계로 함께 전달한다")
    void processProduct_passesVariationListToRegistrationService() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                defaultPolicySetting(),
                List.of(),
                List.of()
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);
        when(productRegistrationService.register(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any(List.class),
                any(List.class),
                anyString(),
                anyString(),
                any(BigDecimal.class),
                anyString(),
                any(BigDecimal.class),
                any(PriceRoundingUnit.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(),
                any()
        )).thenReturn(createRegistration(RegistrationStatus.READY, null));

        ProductProcessingRequest request = createRequest("정상 상품", "브랜드", BigDecimal.valueOf(10));
        List<SourcingVariationResponse> variations = List.of(createVariation("OPT-001"));
        ReflectionTestUtils.setField(request, "sourcingVariations", variations);

        productProcessingService.processProduct(1L, MarketCode.COUPANG, request);

        ArgumentCaptor<List> variationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(productRegistrationService).register(
                eq(1L),
                eq(MarketCode.COUPANG),
                eq("ASIN-001"),
                eq("https://www.amazon.com/dp/ASIN-001"),
                eq("https://image.example/main.jpg"),
                eq(List.of("https://image.example/detail-1.jpg")),
                variationsCaptor.capture(),
                eq("정상 상품"),
                eq("브랜드"),
                eq(BigDecimal.valueOf(10)),
                eq("USD"),
                eq(BigDecimal.valueOf(1350)),
                eq(PriceRoundingUnit.HUNDRED_WON),
                eq(BigDecimal.valueOf(13500)),
                eq(BigDecimal.valueOf(13500)),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.valueOf(3000)),
                eq(RegistrationStatus.READY),
                eq(null)
        );

        assertEquals(1, variationsCaptor.getValue().size());
    }

    @Test
    @DisplayName("수수료율 합계가 100퍼센트 이상이면 예외가 발생하고 저장은 수행하지 않는다")
    void processProduct_throwsException_whenTotalFeeRateIsInvalid() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                policySettingWithFee(BigDecimal.valueOf(70), BigDecimal.valueOf(30)),
                List.of(),
                List.of()
        );

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);

        ProductProcessingRequest request = createRequest("정상 상품", "브랜드", BigDecimal.valueOf(10));

        assertThrows(IllegalArgumentException.class,
                () -> productProcessingService.processProduct(1L, MarketCode.COUPANG, request));

        verify(productRegistrationService, never()).register(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any(List.class),
                any(List.class),
                anyString(),
                anyString(),
                any(BigDecimal.class),
                anyString(),
                any(BigDecimal.class),
                any(PriceRoundingUnit.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(),
                any()
        );
        verify(dummyCoupangProductService, never()).publishAutomatically(anyLong(), anyLong());
    }

    @Test
    @DisplayName("자동 발행 정책이 켜져 있으면 쿠팡 등록 후보 저장 직후 자동 발행한다")
    void processProduct_autoPublishes_whenAutoPublishEnabled() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                policySettingWithAutoPublish(true),
                List.of(),
                List.of()
        );

        DummyProductRegistration savedRegistration = createRegistration(RegistrationStatus.READY, null);
        ReflectionTestUtils.setField(savedRegistration, "dummyProductRegistrationId", 99L);

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);
        when(productRegistrationService.register(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any(List.class),
                any(List.class),
                anyString(),
                anyString(),
                any(BigDecimal.class),
                anyString(),
                any(BigDecimal.class),
                any(PriceRoundingUnit.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(),
                any()
        )).thenReturn(savedRegistration);

        ProductProcessingRequest request = createRequest("정상 상품", "브랜드", BigDecimal.valueOf(10));

        productProcessingService.processProduct(1L, MarketCode.COUPANG, request);

        verify(dummyCoupangProductService).publishAutomatically(1L, 99L);
    }

    @Test
    @DisplayName("자동 발행 정책이 꺼져 있으면 등록 후보만 저장하고 자동 발행하지 않는다")
    void processProduct_doesNotAutoPublish_whenAutoPublishDisabled() {
        PolicyQueryService policyQueryService = mock(PolicyQueryService.class);
        ProductRegistrationService productRegistrationService = mock(ProductRegistrationService.class);
        DummyCoupangProductService dummyCoupangProductService = mock(DummyCoupangProductService.class);
        ProductProcessingService productProcessingService =
                new ProductProcessingService(policyQueryService, productRegistrationService, dummyCoupangProductService, new ProductNameProcessor());

        PolicyBundle policyBundle = new PolicyBundle(
                policySettingWithAutoPublish(false),
                List.of(),
                List.of()
        );

        DummyProductRegistration savedRegistration = createRegistration(RegistrationStatus.READY, null);
        ReflectionTestUtils.setField(savedRegistration, "dummyProductRegistrationId", 99L);

        when(policyQueryService.getPolicyBundle(1L, MarketCode.COUPANG)).thenReturn(policyBundle);
        when(productRegistrationService.register(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                any(List.class),
                any(List.class),
                anyString(),
                anyString(),
                any(BigDecimal.class),
                anyString(),
                any(BigDecimal.class),
                any(PriceRoundingUnit.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(),
                any()
        )).thenReturn(savedRegistration);

        ProductProcessingRequest request = createRequest("정상 상품", "브랜드", BigDecimal.valueOf(10));

        productProcessingService.processProduct(1L, MarketCode.COUPANG, request);

        verify(dummyCoupangProductService, never()).publishAutomatically(anyLong(), anyLong());
    }

    private PolicySettingResponse defaultPolicySetting() {
        return new PolicySettingResponse(
                1L,
                1L,
                MarketCode.COUPANG,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                false,
                false,
                true,
                false,
                true,
                false,
                ShippingFeeType.PAID_SHIPPING,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5000)
        );
    }

    private PolicySettingResponse policySettingWithMinMarginProtect(
            boolean minMarginProtectEnabled,
            BigDecimal targetMarginRate,
            BigDecimal minMarginAmount
    ) {
        return new PolicySettingResponse(
                1L,
                1L,
                MarketCode.COUPANG,
                targetMarginRate,
                minMarginAmount,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(3.3),
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                false,
                false,
                minMarginProtectEnabled,
                false,
                true,
                false,
                ShippingFeeType.PAID_SHIPPING,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5000)
        );
    }

    private PolicySettingResponse policySettingWithFee(BigDecimal marketFeeRate, BigDecimal cardFeeRate) {
        return new PolicySettingResponse(
                1L,
                1L,
                MarketCode.COUPANG,
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(5000),
                marketFeeRate,
                cardFeeRate,
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                false,
                false,
                true,
                false,
                true,
                false,
                ShippingFeeType.PAID_SHIPPING,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5000)
        );
    }

    private PolicySettingResponse policySettingWithAutoPublish(boolean autoPublishEnabled) {
        return new PolicySettingResponse(
                1L,
                1L,
                MarketCode.COUPANG,
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(3.3),
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                false,
                false,
                true,
                false,
                true,
                autoPublishEnabled,
                ShippingFeeType.PAID_SHIPPING,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5000)
        );
    }

    private ProductProcessingRequest createRequest(String translatedProductName, String translatedBrand, BigDecimal originalPrice) {
        ProductProcessingRequest request = new ProductProcessingRequest();
        ReflectionTestUtils.setField(request, "sourceProductId", "ASIN-001");
        ReflectionTestUtils.setField(request, "sourceUrl", "https://www.amazon.com/dp/ASIN-001");
        ReflectionTestUtils.setField(request, "translatedProductName", translatedProductName);
        ReflectionTestUtils.setField(request, "translatedBrand", translatedBrand);
        ReflectionTestUtils.setField(request, "originalPrice", originalPrice);
        ReflectionTestUtils.setField(request, "currency", "USD");
        ReflectionTestUtils.setField(request, "mainImageUrl", "https://image.example/main.jpg");
        ReflectionTestUtils.setField(request, "descriptionImageUrls", List.of("https://image.example/detail-1.jpg"));
        ReflectionTestUtils.setField(request, "sourcingVariations", List.of());
        return request;
    }

    private SourcingVariationResponse createVariation(String asin) {
        SourcingVariationResponse variation = new SourcingVariationResponse();
        ReflectionTestUtils.setField(variation, "asin", asin);
        ReflectionTestUtils.setField(variation, "dimensions", Map.of("Color", "Red"));
        ReflectionTestUtils.setField(variation, "selected", true);
        ReflectionTestUtils.setField(variation, "price", BigDecimal.valueOf(11));
        ReflectionTestUtils.setField(variation, "currency", "USD");
        ReflectionTestUtils.setField(variation, "stock", "In Stock");
        ReflectionTestUtils.setField(variation, "images", List.of("https://image.example/option-red.jpg"));
        return variation;
    }

    private DummyProductRegistration createRegistration(RegistrationStatus registrationStatus, String exclusionReason) {
        return DummyProductRegistration.create(
                1L,
                MarketCode.COUPANG,
                "ASIN-001",
                "https://www.amazon.com/dp/ASIN-001",
                "https://image.example/main.jpg",
                "가공 상품명",
                "브랜드",
                BigDecimal.valueOf(10),
                "USD",
                BigDecimal.valueOf(1350),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                registrationStatus,
                exclusionReason
        );
    }
}
