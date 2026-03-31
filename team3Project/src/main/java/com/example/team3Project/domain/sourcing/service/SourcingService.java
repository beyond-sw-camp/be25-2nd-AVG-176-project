package com.example.team3Project.domain.sourcing.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.entity.SourcingVariation;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;
import com.example.team3Project.domain.sourcing.repository.SourcingVariationRepository;
import com.example.team3Project.domain.user.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SourcingService { 

    private final SourcingRepository sourcingRepository;
    private final SourcingVariationRepository sourcingVariationRepository;
    private final NormalizationService normalizationService;

    // 일단 json 파일이 제대로 원하는 값들이 있는지 확인. 즉, 검증하기
    public List<String> validateSourcingData(SourcingDTO sourcingDTO, User owner) {
        List<String> errors = new ArrayList<>();

        if (owner == null || owner.getId() == null) {
            errors.add("로그인이 필요합니다.");
            return errors;
        }
        
       // json 파일 확인 해보는데 만약 뭔가가 누락이 되었다면, 그 즉시 누락 되었다고 나오기.
        if (!StringUtils.hasText(sourcingDTO.getUrl())) {
            errors.add("url이 누락 되었습니다.");
        } 

        // 상품 ID 확인 (같은 회원이 동일 ASIN을 중복 등록하지 않도록)
        if (!StringUtils.hasText(sourcingDTO.getAsin())) {
            errors.add("asin(productId)이 누락 되었습니다.");
        }
        else if (sourcingRepository.existsByProductIdAndUser_Id(sourcingDTO.getAsin(), owner.getId())) {
            errors.add("이미 내 계정에 등록된 상품입니다.");
        }
        
        if (!StringUtils.hasText(sourcingDTO.getTitle())) {
            errors.add("title이 누락 되었습니다.");
        }
        
        if (sourcingDTO.getPrice() == null) {
            // price 없으면 variation에서 최솟값으로 대체 가능한지 확인
            boolean hasVariationPrice = sourcingDTO.getVariation() != null &&
                sourcingDTO.getVariation().stream()
                    .anyMatch(v -> v.getPrice() != null && v.getPrice().compareTo(BigDecimal.ZERO) > 0);
            if (!hasVariationPrice) {
                errors.add("price가 누락 되었습니다. (variation에도 price가 없습니다)");
            }
        } else if (sourcingDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("상품 가격은 0보다 커야 합니다.");
        }

        if (!StringUtils.hasText(sourcingDTO.getCurrency())) {
            errors.add("currency가 누락 되었습니다.");
        }

        if (!StringUtils.hasText(sourcingDTO.getBrand())){
            errors.add("brand가 누락 되었습니다.");
        }
        // 메인 이미지
        if (!StringUtils.hasText(sourcingDTO.getUrlImage())) {
            errors.add("url_image가 누락 되었습니다.");
        }
        // 부가 이미지들.
        if (sourcingDTO.getImages() == null || sourcingDTO.getImages().isEmpty()) {
            errors.add("images가 누락 되었습니다.");
        }

        return errors;    
    }

    // 일단 variation 제대로 되는지 확인 해보기 위한 테스트 메소드
    // @Transactional
    // public Sourcing getSourcingWithVariations(Long id) {
    //     return sourcingRepository.findByIdWithVariations(id)
    //             .orElseThrow(() -> new RuntimeException("상품 없음"));
    // }

    @Transactional
    public Long saveSourcingData(SourcingDTO sourcingDTO, User owner) {
        if (owner == null || owner.getId() == null) {
            throw new IllegalStateException("로그인 사용자 정보가 없습니다.");
        }

        // 상대 경로인 url을 아마존 절대 경로로 변환
        String fullAmazonUrl = "https://www.amazon.com" + sourcingDTO.getUrl();

        // price가 없으면 variation 중 최솟값으로 대체
        BigDecimal effectivePrice = sourcingDTO.getPrice();
        if (effectivePrice == null && sourcingDTO.getVariation() != null) {
            effectivePrice = sourcingDTO.getVariation().stream()
                .map(SourcingDTO.VariationDTO::getPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);
        }

        // 소싱 데이터 저장.
        Sourcing sourcing = Sourcing.builder()
                .user(owner)
                .sourceUrl(fullAmazonUrl)
                .siteName("Amazon")
                .productId(sourcingDTO.getAsin())
                .title(sourcingDTO.getTitle())
                .originalPrice(effectivePrice)
                .currency(sourcingDTO.getCurrency())
                .brand(sourcingDTO.getBrand())
                .mainImageUrl(sourcingDTO.getUrlImage())
                .descriptionImages(sourcingDTO.getImages())
                .build();

        sourcingRepository.save(sourcing);

        // 소싱의 variation 저장
        if (sourcingDTO.getVariation() != null) {
            for (SourcingDTO.VariationDTO varDTO : sourcingDTO.getVariation()) {
                SourcingVariation variation = SourcingVariation.builder()
                        .sourcing(sourcing)
                        .asin(varDTO.getAsin())
                        .dimensions(varDTO.getDimensions())
                        .selected(varDTO.isSelected())
                        .price(varDTO.getPrice())
                        .currency(varDTO.getCurrency())
                        .stock(varDTO.getStock())
                        .rating(varDTO.getRating())
                        .reviewsCount(varDTO.getReviewsCount())
                        .images(varDTO.getImages())
                        .build();
                sourcingVariationRepository.save(variation);
            }
        }

        return sourcing.getId();
    }

    
     //* 저장 후 정규화 시도.
    // * 정규화 실패 시에도 소싱 행은 유지하고 {@link SourcingRegistrationStatus#NORMALIZATION_FAILED}로 표시한다.
     
    @Transactional
    public SourcingPersistOutcome saveSourcingDataAndNormalize(SourcingDTO sourcingDTO, User owner) {
        Long id = saveSourcingData(sourcingDTO, owner);
        try {
            normalizationService.normalize(id);
        } catch (RuntimeException ex) {
            sourcingRepository.findById(id).ifPresent(s ->
                    s.setRegistrationStatus(SourcingRegistrationStatus.NORMALIZATION_FAILED));
            return new SourcingPersistOutcome(id, SourcingRegistrationStatus.NORMALIZATION_FAILED, ex.getMessage());
        }
        return new SourcingPersistOutcome(id, SourcingRegistrationStatus.NORMALIZED, null);
    }

}