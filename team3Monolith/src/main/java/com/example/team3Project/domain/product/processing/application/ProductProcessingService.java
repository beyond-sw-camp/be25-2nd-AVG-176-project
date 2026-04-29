package com.example.team3Project.domain.product.processing.application;

import com.example.team3Project.domain.policy.application.PolicyQueryService;
import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.policy.dto.PolicyBundle;
import com.example.team3Project.domain.policy.dto.ProductNameProcessingResponse;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.ShippingFeeType;
import com.example.team3Project.domain.product.coupang.application.DummyCoupangProductService;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingRequest;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingResultResponse;
import com.example.team3Project.domain.product.registration.application.ProductRegistrationService;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 상품 가공 서비스 클래스
public class ProductProcessingService {

    private final PolicyQueryService policyQueryService;
    private final ProductRegistrationService productRegistrationService;
    private final DummyCoupangProductService dummyCoupangProductService;
    private final ProductNameProcessor productNameProcessor;

    // 가공에 사용할 정책 묶음을 가져오는 메서드
    public PolicyBundle getPolicyBundleForProcessing(Long userId, MarketCode marketCode) {

        return policyQueryService.getPolicyBundle(userId, marketCode);
    }

    // 금지어 포함 여부를 검사하는 메서드
    // productName : 검사할 상품명
    // policyBundle : 가공에 사용할 정책 묶음
    public boolean containsBlockedWord(String productName, PolicyBundle policyBundle) {
        if (productNameProcessor != null) {
            return productNameProcessor.containsBlockedWord(productName, policyBundle);
        }
        for (BlockedWordResponse blockedWord : policyBundle.getBlockedWords()) {
            if (productName.contains(blockedWord.getBlockedWord())) {
                return true;    // 금지어가 포함되는 경우
            }
        }
        return false;    // 모든 금지어를 검사했으나 포함되지 않는 경우
    }

    // 치환어를 적용하여 가공된 상품명을 반환하는 메서드
    // productName : 검사할 상품명
    // policyBundle : 가공에 사용할 정책 묶음
    public String applyReplacementWords(String productName, PolicyBundle policyBundle) {
        if (productNameProcessor != null) {
            return productNameProcessor.applyReplacementWords(productName, policyBundle);
        }
        String processedName = productName;

        // 치환어 적용 - List로 받은 치환어에 대해 for문으로 돌려 작업한다.
        for (ReplacementWordResponse replacementWord : policyBundle.getReplacementWords()) {
            processedName = processedName.replace(
                    // 원본 단어
                    replacementWord.getSourceWord(),
                    // 새 단어
                    replacementWord.getReplacementWord()
            );
        }
        return processedName;
    }


    // 상품명을 가공하는 메서드
    // 가공이 가능한 경우 가공된 상품명을 반환, 금지어로 제외되는 경우 값을 비워서 반환
    public Optional<String> processProductName(Long userId, MarketCode marketCode, String productName) {
        PolicyBundle policyBundle = getPolicyBundleForProcessing(userId, marketCode);
        if (productNameProcessor != null) {
            return productNameProcessor.processProductName(policyBundle, productName);
        }

        // 금지어가 포함되어 있는지 검사
        if (containsBlockedWord(productName, policyBundle)) {
            return Optional.empty();
        }

        // 치환어 적용
        String processedName = applyReplacementWords(productName, policyBundle);
        // 가공된 상품명 Optional에 담아서 반환
        return Optional.of(processedName);
    }

    // 상품 가공 메서드
    @Transactional
    public ProductProcessingResultResponse processProduct(Long userId, MarketCode marketCode, @Valid ProductProcessingRequest request) {
        // 정책 번들 가져오기
        PolicyBundle policyBundle = getPolicyBundleForProcessing(userId, marketCode);

        // 상품명 금지어 조회
        String productName = request.getTranslatedProductName();
        boolean excluded = containsBlockedWord(productName, policyBundle);

        // 상품명 치환어 적용
        String processedProductName = applyReplacementWords(productName, policyBundle);

        // 브랜드
        // 일단 가공 전 번역된 상품명 적용
        String processedBrand = request.getTranslatedBrand();
        BigDecimal exchangeRate = policyBundle.getPolicySettingResponse().getExchangeRate();

        BigDecimal costInKrw = BigDecimal.ZERO;
        BigDecimal salePrice = BigDecimal.ZERO;
        BigDecimal marginKrw = BigDecimal.ZERO;
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal totalFeeRate = BigDecimal.ZERO;

        // 상품 등록 상태
        RegistrationStatus registrationStatus = excluded
                ? RegistrationStatus.BLOCKED
                : RegistrationStatus.READY;
        // 가공 시 제외 사유
        String exclusionReason = excluded ? "BLOCKED_WORD" : null;

        if (!excluded) {
            // 배송비 계산 - 배송비 유형에 따라 계산
            shippingFee =
                    policyBundle.getPolicySettingResponse().getShippingFeeType() == ShippingFeeType.FREE_SHIPPING
                            ? BigDecimal.ZERO
                            : policyBundle.getPolicySettingResponse().getBaseShippingFee();

            BigDecimal targetMarginRate = policyBundle.getPolicySettingResponse().getTargetMarginRate();
            BigDecimal minMarginAmount = policyBundle.getPolicySettingResponse().getMinMarginAmount();
            BigDecimal marketFeeRate = policyBundle.getPolicySettingResponse().getMarketFeeRate();
            BigDecimal cardFeeRate = policyBundle.getPolicySettingResponse().getCardFeeRate();

            // 원가 원화로 환산
            costInKrw = request.getOriginalPrice().multiply(exchangeRate);

            // 목표 마진 금액 계산
            BigDecimal marginByRate = costInKrw
                    .multiply(targetMarginRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // 정책 상 최소 마진 보호 여부
            boolean minMarginProtectEnabled =
                    policyBundle.getPolicySettingResponse().isMinMarginProtectEnabled();

            // 실제 적용 마진 - 최소 마진 보호 여부에 따라
            BigDecimal margin = minMarginProtectEnabled
                    ? marginByRate.max(minMarginAmount)
                    : marginByRate;

            // 수수료 계산
            totalFeeRate = marketFeeRate.add(cardFeeRate);

            BigDecimal feeMultiplier = BigDecimal.ONE.subtract(
                    totalFeeRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            // 수수료가 비정상적인지 확인(마켓 수수료 + 카드 수수료가 100%인지 확인)
            if (feeMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("수수료율 합계가 100% 이상이면 판매가를 계산할 수 없습니다.");
            }


            // 판매가 계산
            salePrice = costInKrw.add(margin)
                    .divide(feeMultiplier, 0, RoundingMode.UP);


            // 사용자 지정 올림 단위
            BigDecimal roundingUnit = BigDecimal.valueOf(
                    policyBundle.getPolicySettingResponse().getRoundingUnit().getAmount()
            );

            // 사용자 지정 올림 단위 반영
            salePrice = salePrice
                    .divide(roundingUnit, 0, RoundingMode.UP)
                    .multiply(roundingUnit);

            // 최종 판매가 기준 원화 마진이다. 배송비는 별도 필드로 관리하므로 여기서는 제외한다.
            marginKrw = calculateMarginKrw(salePrice, costInKrw, totalFeeRate);
        }

        // 최종 가공 결과 저장
        DummyProductRegistration savedRegistration =
                productRegistrationService.register(
                        userId,
                        marketCode,
                        request.getSourceProductId(),
                        request.getSourceUrl(),
                        request.getMainImageUrl(),
                        request.getDescriptionImageUrls(),
                        request.getSourcingVariations(),
                        processedProductName,
                        processedBrand,
                        request.getOriginalPrice(),
                        request.getCurrency(),
                        exchangeRate,
                        policyBundle.getPolicySettingResponse().getRoundingUnit(),
                        costInKrw,
                        salePrice,
                        marginKrw,
                        shippingFee,
                        registrationStatus,
                        exclusionReason
                );

        autoPublishIfEnabled(policyBundle, savedRegistration);

        return new ProductProcessingResultResponse(
                excluded,
                exclusionReason,
                processedProductName,
                processedBrand,
                request.getOriginalPrice(),
                request.getCurrency(),
                exchangeRate, // 우선 사용자 설정 값
                excluded ? null : costInKrw,
                excluded ? null : salePrice,
                excluded ? null : shippingFee,
                savedRegistration.getRegistrationStatus().name()
        );
    }

    private BigDecimal calculateMarginKrw(BigDecimal salePrice, BigDecimal costInKrw, BigDecimal totalFeeRate) {
        BigDecimal feeAmount = salePrice
                .multiply(totalFeeRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.UP);

        return salePrice.subtract(costInKrw).subtract(feeAmount);
    }

    private void autoPublishIfEnabled(PolicyBundle policyBundle, DummyProductRegistration savedRegistration) {
        if (!policyBundle.getPolicySettingResponse().isAutoPublishEnabled()) {
            return;
        }

        if (savedRegistration.getRegistrationStatus() != RegistrationStatus.READY) {
            return;
        }

        if (savedRegistration.getMarketCode() == MarketCode.COUPANG) {
            dummyCoupangProductService.publishAutomatically(
                    savedRegistration.getUserId(),
                    savedRegistration.getDummyProductRegistrationId()
            );
        }
    }

    // 상품명 가공 결과를 응답 DTO에 담는 메서드 - 나중에 제외시킬 수도 있음
    public ProductNameProcessingResponse processProductNameResponse(Long userId, MarketCode marketCode, String
            productName) {
        Optional<String> processedResult = processProductName(userId, marketCode, productName);

        // 금지어가 포함되어 있어 가공에서 제외된 경우
        if (processedResult.isEmpty()) {
            return new ProductNameProcessingResponse(true, null);
        }

        // 가공 결과를 반환
        return new ProductNameProcessingResponse(false, processedResult.get());
    }

}
