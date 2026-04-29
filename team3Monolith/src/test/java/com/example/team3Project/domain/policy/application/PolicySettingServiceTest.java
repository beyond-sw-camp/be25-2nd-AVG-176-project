package com.example.team3Project.domain.policy.application;

import com.example.team3Project.domain.policy.dao.UserPolicySettingRepository;
import com.example.team3Project.domain.policy.dto.PolicySettingResponse;
import com.example.team3Project.domain.policy.dto.PolicySettingUpsertRequest;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.policy.entity.ShippingFeeType;
import com.example.team3Project.domain.policy.entity.UserPolicySetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicySettingServiceTest {

    private UserPolicySettingRepository repository;
    private PolicySettingService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserPolicySettingRepository.class);
        service = new PolicySettingService(repository);
    }

    @Test
    @DisplayName("정책 설정이 없으면 PUT 요청 기준으로 새 설정을 생성한다")
    void upsertPolicySetting_createsSetting_whenMissing() {
        PolicySettingUpsertRequest request = request(BigDecimal.valueOf(30), BigDecimal.valueOf(8000));

        when(repository.findByUserIdAndMarketCode(1L, MarketCode.COUPANG))
                .thenReturn(Optional.empty());
        when(repository.save(any(UserPolicySetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PolicySettingResponse response = service.upsertPolicySetting(1L, MarketCode.COUPANG, request);

        assertEquals(MarketCode.COUPANG, response.getMarketCode());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(response.getTargetMarginRate()));
        assertEquals(0, BigDecimal.valueOf(8000).compareTo(response.getMinMarginAmount()));
        verify(repository).save(any(UserPolicySetting.class));
    }

    @Test
    @DisplayName("정책 설정이 있으면 기존 설정 값을 수정한다")
    void upsertPolicySetting_updatesSetting_whenExists() {
        UserPolicySetting existing = UserPolicySetting.create(
                1L,
                MarketCode.COUPANG,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(1300),
                PriceRoundingUnit.HUNDRED_WON,
                false,
                false,
                true,
                false,
                false,
                false,
                ShippingFeeType.PAID_SHIPPING,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(6000)
        );
        PolicySettingUpsertRequest request = request(BigDecimal.valueOf(35), BigDecimal.valueOf(9000));

        when(repository.findByUserIdAndMarketCode(1L, MarketCode.COUPANG))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        PolicySettingResponse response = service.upsertPolicySetting(1L, MarketCode.COUPANG, request);

        assertEquals(MarketCode.COUPANG, response.getMarketCode());
        assertEquals(0, BigDecimal.valueOf(35).compareTo(response.getTargetMarginRate()));
        assertEquals(0, BigDecimal.valueOf(9000).compareTo(response.getMinMarginAmount()));
        verify(repository).save(existing);
    }

    private PolicySettingUpsertRequest request(BigDecimal targetMarginRate, BigDecimal minMarginAmount) {
        PolicySettingUpsertRequest request = new PolicySettingUpsertRequest();
        ReflectionTestUtils.setField(request, "targetMarginRate", targetMarginRate);
        ReflectionTestUtils.setField(request, "minMarginAmount", minMarginAmount);
        ReflectionTestUtils.setField(request, "marketFeeRate", BigDecimal.valueOf(10));
        ReflectionTestUtils.setField(request, "cardFeeRate", BigDecimal.valueOf(3.3));
        ReflectionTestUtils.setField(request, "exchangeRate", BigDecimal.valueOf(1350));
        ReflectionTestUtils.setField(request, "roundingUnit", PriceRoundingUnit.HUNDRED_WON);
        ReflectionTestUtils.setField(request, "amazonAutoPricingEnabled", true);
        ReflectionTestUtils.setField(request, "competitorAutoPricingEnabled", true);
        ReflectionTestUtils.setField(request, "minMarginProtectEnabled", true);
        ReflectionTestUtils.setField(request, "priceAutoUpdateEnabled", true);
        ReflectionTestUtils.setField(request, "stopLossEnabled", true);
        ReflectionTestUtils.setField(request, "autoPublishEnabled", true);
        ReflectionTestUtils.setField(request, "shippingFeeType", ShippingFeeType.PAID_SHIPPING);
        ReflectionTestUtils.setField(request, "baseShippingFee", BigDecimal.valueOf(5000));
        ReflectionTestUtils.setField(request, "remoteAreaExtraShippingFee", BigDecimal.valueOf(8000));
        ReflectionTestUtils.setField(request, "returnShippingFee", BigDecimal.valueOf(10000));
        return request;
    }
}
