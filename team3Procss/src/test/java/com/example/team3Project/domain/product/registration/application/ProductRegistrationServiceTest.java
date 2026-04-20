package com.example.team3Project.domain.product.registration.application;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.product.coupang.application.DummyCoupangProductService;
import com.example.team3Project.domain.product.processing.dto.SourcingVariationResponse;
import com.example.team3Project.domain.product.registration.dao.DummyProductRegistrationRepository;
import com.example.team3Project.domain.product.registration.dto.DummyProductRegistrationStatusCountsResponse;
import com.example.team3Project.domain.product.registration.entity.DummyProductImageType;
import com.example.team3Project.domain.product.registration.entity.DummyProductOption;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import com.example.team3Project.domain.sourcing.SourcingCleanupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductRegistrationServiceTest {

    private DummyProductRegistrationRepository repository;
    private DummyCoupangProductService dummyCoupangProductService;
    private SourcingCleanupRepository sourcingCleanupRepository;
    private ProductRegistrationService service;

    @BeforeEach
    void setUp() {
        repository = mock(DummyProductRegistrationRepository.class);
        dummyCoupangProductService = mock(DummyCoupangProductService.class);
        sourcingCleanupRepository = mock(SourcingCleanupRepository.class);
        service = new ProductRegistrationService(
                repository,
                dummyCoupangProductService,
                sourcingCleanupRepository,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("같은 사용자와 같은 상품이 다시 등록되면 기존 등록 건을 갱신한다")
    void register_updatesExistingRegistration_whenSameUserAndSourceProduct() {
        DummyProductRegistration existing = DummyProductRegistration.create(
                1L,
                MarketCode.COUPANG,
                "ASIN-001",
                "old-url",
                "old-main",
                "old-name",
                "old-brand",
                BigDecimal.valueOf(10),
                "USD",
                BigDecimal.valueOf(1300),
                BigDecimal.valueOf(13000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(3000),
                RegistrationStatus.READY,
                null
        );
        ReflectionTestUtils.setField(existing, "dummyProductRegistrationId", 99L);

        when(repository.findByUserIdAndMarketCodeAndSourceProductId(1L, MarketCode.COUPANG, "ASIN-001"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        DummyProductRegistration result = service.register(
                1L,
                MarketCode.COUPANG,
                "ASIN-001",
                "new-url",
                "new-main",
                List.of("desc-1"),
                List.of(),
                "new-name",
                "new-brand",
                BigDecimal.valueOf(20),
                "USD",
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                BigDecimal.valueOf(27000),
                BigDecimal.valueOf(35000),
                BigDecimal.valueOf(4000),
                RegistrationStatus.BLOCKED,
                "BLOCKED_WORD"
        );

        assertSame(existing, result);
        assertEquals("new-url", existing.getSourceUrl());
        assertEquals("new-name", existing.getProcessedProductName());
        assertEquals(RegistrationStatus.BLOCKED, existing.getRegistrationStatus());
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("MinIO objectKey 이미지에서 대표/상세/옵션 이미지를 구분해 저장한다")
    void register_savesMinioObjectKeysByImageType() {
        when(repository.findByUserIdAndMarketCodeAndSourceProductId(1L, MarketCode.COUPANG, "B0MAIN"))
                .thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(DummyProductRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SourcingVariationResponse variation = new SourcingVariationResponse();
        ReflectionTestUtils.setField(variation, "asin", "B0OPTION");
        ReflectionTestUtils.setField(variation, "images", List.of("1/10001/var/B0OPTION/0.jpeg"));

        DummyProductRegistration result = service.register(
                1L,
                MarketCode.COUPANG,
                "B0MAIN",
                "https://www.amazon.com/dp/B0MAIN",
                "1/10001/desc/B0MAIN/0.jpeg",
                List.of("1/10001/desc/B0MAIN/0.jpeg", "1/10001/desc/B0MAIN/1.jpeg"),
                List.of(variation),
                "processed-name",
                "processed-brand",
                BigDecimal.valueOf(20),
                "USD",
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                BigDecimal.valueOf(27000),
                BigDecimal.valueOf(35000),
                BigDecimal.valueOf(4000),
                RegistrationStatus.READY,
                null
        );

        assertEquals(3, result.getImages().size());
        assertEquals(DummyProductImageType.MAIN, result.getImages().get(0).getImageType());
        assertEquals("1/10001/desc/B0MAIN/0.jpeg", result.getImages().get(0).getObjectKey());
        assertEquals(DummyProductImageType.DESCRIPTION, result.getImages().get(1).getImageType());
        assertEquals(1, result.getImages().get(1).getSortOrder());
        assertEquals(DummyProductImageType.OPTION, result.getImages().get(2).getImageType());
        assertEquals("B0OPTION", result.getImages().get(2).getOptionAsin());
        assertEquals("sourcing-images", result.getImages().get(2).getBucketName());
    }

    @Test
    @DisplayName("option sale price applies policy exchange rate")
    void register_appliesExchangeRateToOptionSalePrice() {
        when(repository.findByUserIdAndMarketCodeAndSourceProductId(1L, MarketCode.COUPANG, "B0MAIN"))
                .thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(DummyProductRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SourcingVariationResponse variation = new SourcingVariationResponse();
        ReflectionTestUtils.setField(variation, "asin", "B0OPTION");
        ReflectionTestUtils.setField(variation, "dimensions", Map.of("Color", "Black"));
        ReflectionTestUtils.setField(variation, "selected", true);
        ReflectionTestUtils.setField(variation, "price", BigDecimal.valueOf(12.5));
        ReflectionTestUtils.setField(variation, "currency", "USD");
        ReflectionTestUtils.setField(variation, "stock", "In Stock");

        DummyProductRegistration result = service.register(
                1L,
                MarketCode.COUPANG,
                "B0MAIN",
                "https://www.amazon.com/dp/B0MAIN",
                "main",
                List.of(),
                List.of(variation),
                "processed-name",
                "processed-brand",
                BigDecimal.valueOf(20),
                "USD",
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                BigDecimal.valueOf(27000),
                BigDecimal.valueOf(35000),
                BigDecimal.valueOf(4000),
                RegistrationStatus.READY,
                null
        );

        DummyProductOption option = result.getOptions().get(0);
        assertEquals(0, BigDecimal.valueOf(12.5).compareTo(option.getOriginalPrice()));
        assertEquals(0, BigDecimal.valueOf(16900).compareTo(option.getSalePrice()));
    }

    @Test
    @DisplayName("등록 삭제는 로그인 사용자 소유 데이터만 삭제한다")
    void deleteRegistration_deletesOwnedRegistration() {
        DummyProductRegistration registration = DummyProductRegistration.create(
                1L,
                MarketCode.COUPANG,
                "ASIN-001",
                "url",
                "main",
                "name",
                "brand",
                BigDecimal.TEN,
                "USD",
                BigDecimal.valueOf(1350),
                BigDecimal.valueOf(13500),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(3000),
                RegistrationStatus.READY,
                null
        );

        when(repository.findByDummyProductRegistrationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(registration));

        service.deleteRegistration(1L, 10L);

        verify(dummyCoupangProductService).deleteBySourceProductId(1L, "ASIN-001");
        verify(repository).delete(registration);
    }

    @Test
    @DisplayName("등록 삭제 대상이 없으면 404를 반환한다")
    void deleteRegistration_throwsNotFound_whenRegistrationMissing() {
        when(repository.findByDummyProductRegistrationIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteRegistration(1L, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("등록 상태별 개수를 사용자 기준으로 집계한다")
    void getStatusCounts_returnsCountsByStatus() {
        when(repository.countByUserIdAndRegistrationStatusAndReceivedAtRange(1L, RegistrationStatus.READY, null, null))
                .thenReturn(3L);
        when(repository.countByUserIdAndRegistrationStatusAndReceivedAtRange(1L, RegistrationStatus.REGISTERED, null, null))
                .thenReturn(5L);
        when(repository.countByUserIdAndRegistrationStatusAndReceivedAtRange(1L, RegistrationStatus.FAILED, null, null))
                .thenReturn(2L);
        when(repository.countByUserIdAndRegistrationStatusAndReceivedAtRange(1L, RegistrationStatus.BLOCKED, null, null))
                .thenReturn(1L);
        when(repository.countByUserIdAndRegistrationStatusInAndReceivedAtRange(
                1L,
                List.of(RegistrationStatus.FAILED, RegistrationStatus.BLOCKED),
                null,
                null
        )).thenReturn(3L);
        when(repository.countByUserIdAndReceivedAtRange(1L, null, null)).thenReturn(11L);

        DummyProductRegistrationStatusCountsResponse response =
                service.getStatusCounts(1L, null, null);

        assertEquals(3L, response.getReadyCount());
        assertEquals(5L, response.getRegisteredCount());
        assertEquals(3L, response.getFailedOrCanceledCount());
        assertEquals(11L, response.getTotalCount());
        assertEquals(2L, response.getFailedCount());
        assertEquals(1L, response.getCanceledCount());
    }

    @Test
    @DisplayName("등록 다건 삭제는 선택한 항목이 모두 로그인 사용자 소유일 때만 삭제한다")
    void deleteRegistrations_deletesSelectedRegistrations() {
        DummyProductRegistration first = DummyProductRegistration.create(
                1L, MarketCode.COUPANG, "ASIN-001", "url1", "main1", "name1", "brand1",
                BigDecimal.TEN, "USD", BigDecimal.valueOf(1350), BigDecimal.valueOf(13500),
                BigDecimal.valueOf(20000), BigDecimal.valueOf(3000), RegistrationStatus.READY, null
        );
        DummyProductRegistration second = DummyProductRegistration.create(
                1L, MarketCode.COUPANG, "ASIN-002", "url2", "main2", "name2", "brand2",
                BigDecimal.TEN, "USD", BigDecimal.valueOf(1350), BigDecimal.valueOf(13500),
                BigDecimal.valueOf(21000), BigDecimal.valueOf(3000), RegistrationStatus.READY, null
        );

        when(repository.findAllByDummyProductRegistrationIdInAndUserId(List.of(10L, 11L), 1L))
                .thenReturn(List.of(first, second));

        service.deleteRegistrations(1L, List.of(10L, 11L));

        verify(dummyCoupangProductService).deleteBySourceProductIds(1L, List.of("ASIN-001", "ASIN-002"));
        verify(repository).deleteAll(List.of(first, second));
    }

    @Test
    @DisplayName("등록 상태를 REGISTERED로 바꾸면 쿠팡 더미 상품으로 발행한다")
    void updateRegistrationStatus_publishesCoupangProduct_whenRegistered() {
        DummyProductRegistration registration = DummyProductRegistration.create(
                1L, MarketCode.COUPANG, "ASIN-001", "url1", "main1", "name1", "brand1",
                BigDecimal.TEN, "USD", BigDecimal.valueOf(1350), BigDecimal.valueOf(13500),
                BigDecimal.valueOf(20000), BigDecimal.valueOf(3000), RegistrationStatus.READY, null
        );

        when(repository.findByDummyProductRegistrationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(registration));

        service.updateRegistrationStatus(1L, 10L, RegistrationStatus.REGISTERED, null);

        verify(dummyCoupangProductService).publishAutomatically(1L, 10L);
    }

    @Test
    @DisplayName("등록 상태를 판매 중지로 바꾸면 연결된 쿠팡 더미 상품을 삭제한다")
    void updateRegistrationStatus_deletesCoupangProduct_whenBlocked() {
        DummyProductRegistration registration = DummyProductRegistration.create(
                1L, MarketCode.COUPANG, "ASIN-001", "url1", "main1", "name1", "brand1",
                BigDecimal.TEN, "USD", BigDecimal.valueOf(1350), BigDecimal.valueOf(13500),
                BigDecimal.valueOf(20000), BigDecimal.valueOf(3000), RegistrationStatus.REGISTERED, null
        );

        when(repository.findByDummyProductRegistrationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(registration));

        service.updateRegistrationStatus(1L, 10L, RegistrationStatus.BLOCKED, "sold out");

        assertEquals(RegistrationStatus.BLOCKED, registration.getRegistrationStatus());
        assertEquals("sold out", registration.getExclusionReason());
        verify(dummyCoupangProductService).deleteBySourceProductId(1L, "ASIN-001");
    }

    @Test
    @DisplayName("등록 상태를 READY로 되돌리면 쿠팡 더미 상품에서 삭제하고 등록 대기 상태로 복구한다")
    void updateRegistrationStatus_deletesCoupangProduct_whenReady() {
        DummyProductRegistration registration = DummyProductRegistration.create(
                1L, MarketCode.COUPANG, "ASIN-001", "url1", "main1", "name1", "brand1",
                BigDecimal.TEN, "USD", BigDecimal.valueOf(1350), BigDecimal.valueOf(13500),
                BigDecimal.valueOf(20000), BigDecimal.valueOf(3000), RegistrationStatus.REGISTERED, null
        );

        when(repository.findByDummyProductRegistrationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(registration));

        service.updateRegistrationStatus(1L, 10L, RegistrationStatus.READY, null);

        assertEquals(RegistrationStatus.READY, registration.getRegistrationStatus());
        verify(dummyCoupangProductService).deleteBySourceProductId(1L, "ASIN-001");
    }

    @Test
    @DisplayName("등록 다건 삭제 대상 중 일부라도 없으면 404를 반환한다")
    void deleteRegistrations_throwsNotFound_whenSomeRegistrationsMissing() {
        DummyProductRegistration first = DummyProductRegistration.create(
                1L, MarketCode.COUPANG, "ASIN-001", "url1", "main1", "name1", "brand1",
                BigDecimal.TEN, "USD", BigDecimal.valueOf(1350), BigDecimal.valueOf(13500),
                BigDecimal.valueOf(20000), BigDecimal.valueOf(3000), RegistrationStatus.READY, null
        );

        when(repository.findAllByDummyProductRegistrationIdInAndUserId(List.of(10L, 11L), 1L))
                .thenReturn(List.of(first));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteRegistrations(1L, List.of(10L, 11L))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
