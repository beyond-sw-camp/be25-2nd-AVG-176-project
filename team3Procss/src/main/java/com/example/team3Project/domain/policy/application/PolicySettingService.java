package com.example.team3Project.domain.policy.application;

import com.example.team3Project.domain.policy.dao.UserPolicySettingRepository;
import com.example.team3Project.domain.policy.dto.PolicySettingResponse;
import com.example.team3Project.domain.policy.dto.PolicySettingUpsertRequest;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.UserPolicySetting;
import com.example.team3Project.domain.policy.exception.PolicySettingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolicySettingService {

    private final UserPolicySettingRepository userPolicySettingRepository;

    public PolicySettingResponse upsertPolicySetting(
            Long userId,
            MarketCode marketCode,
            PolicySettingUpsertRequest request
    ) {
        // 사용자와 마켓 조합의 설정이 있으면 수정하고, 없으면 새 정책 설정을 만든다.
        UserPolicySetting setting = userPolicySettingRepository.findByUserIdAndMarketCode(userId, marketCode)
                .map(existing -> {
                    existing.update(
                            request.getTargetMarginRate(),
                            request.getMinMarginAmount(),
                            request.getMarketFeeRate(),
                            request.getCardFeeRate(),
                            request.getExchangeRate(),
                            request.getRoundingUnit(),
                            request.isAmazonAutoPricingEnabled(),
                            request.isCompetitorAutoPricingEnabled(),
                            request.isMinMarginProtectEnabled(),
                            request.isPriceAutoUpdateEnabled(),
                            request.isStopLossEnabled(),
                            request.isAutoPublishEnabled(),
                            request.getShippingFeeType(),
                            request.getBaseShippingFee(),
                            request.getRemoteAreaExtraShippingFee(),
                            request.getReturnShippingFee()
                    );
                    return existing;
                })
                .orElseGet(() -> UserPolicySetting.create(
                        userId,
                        marketCode,
                        request.getTargetMarginRate(),
                        request.getMinMarginAmount(),
                        request.getMarketFeeRate(),
                        request.getCardFeeRate(),
                        request.getExchangeRate(),
                        request.getRoundingUnit(),
                        request.isAmazonAutoPricingEnabled(),
                        request.isCompetitorAutoPricingEnabled(),
                        request.isMinMarginProtectEnabled(),
                        request.isPriceAutoUpdateEnabled(),
                        request.isStopLossEnabled(),
                        request.isAutoPublishEnabled(),
                        request.getShippingFeeType(),
                        request.getBaseShippingFee(),
                        request.getRemoteAreaExtraShippingFee(),
                        request.getReturnShippingFee()
                ));

        UserPolicySetting saved = userPolicySettingRepository.save(setting);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PolicySettingResponse getPolicySetting(Long userId, MarketCode marketCode) {
        UserPolicySetting setting = userPolicySettingRepository.findByUserIdAndMarketCode(userId, marketCode)
                .orElseThrow(() -> new PolicySettingNotFoundException("정책 설정이 존재하지 않습니다."));

        return toResponse(setting);
    }

    private PolicySettingResponse toResponse(UserPolicySetting setting) {
        return new PolicySettingResponse(
                setting.getUserPolicySettingId(),
                setting.getUserId(),
                setting.getMarketCode(),
                setting.getTargetMarginRate(),
                setting.getMinMarginAmount(),
                setting.getMarketFeeRate(),
                setting.getCardFeeRate(),
                setting.getExchangeRate(),
                setting.getRoundingUnit(),
                setting.isAmazonAutoPricingEnabled(),
                setting.isCompetitorAutoPricingEnabled(),
                setting.isMinMarginProtectEnabled(),
                setting.isPriceAutoUpdateEnabled(),
                setting.isStopLossEnabled(),
                setting.isAutoPublishEnabled(),
                setting.getShippingFeeType(),
                setting.getBaseShippingFee(),
                setting.getRemoteAreaExtraShippingFee(),
                setting.getReturnShippingFee()
        );
    }
}
