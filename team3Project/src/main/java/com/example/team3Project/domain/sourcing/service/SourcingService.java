package com.example.team3Project.domain.sourcing.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.entity.SourcingVariation;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;
import com.example.team3Project.domain.sourcing.repository.SourcingVariationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SourcingService { 

    private final SourcingRepository sourcingRepository;
    private final SourcingVariationRepository sourcingVariationRepository;

    // 일단 json 파일이 제대로 원하는 값들이 있는지 확인. 즉, 검증하기
    public List<String> validateSourcingData(SourcingDTO sourcingDTO) {
        List<String> errors = new ArrayList<>();
        
       // json 파일 확인 해보는데 만약 뭔가가 누락이 되었다면, 그 즉시 누락 되었다고 나오기.
        if (!StringUtils.hasText(sourcingDTO.getUrl())) {
            errors.add("url이 누락 되었습니다.");
        } 

        // 상품 ID 확인
        if (!StringUtils.hasText(sourcingDTO.getAsin())) {
            errors.add("asin(productId)이 누락 되었습니다.");
        }
        else if (sourcingRepository.existsByProductId(sourcingDTO.getAsin())) {
            errors.add("이미 등록된 상품입니다.");
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
    public void saveSourcingData(SourcingDTO sourcingDTO) {

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

    }

}