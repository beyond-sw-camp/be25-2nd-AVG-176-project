package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SourcingService { 

    // MariaDB에 저장하기 위한 하나의 객체.
    private final SourcingRepository sourcingRepository;

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
            errors.add("price가 누락 되었습니다.");
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

    @Transactional
    // 테이블 생성 및 옵션 테이블에 데이터 추가.
    public void saveSourcingData(SourcingDTO sourcingDTO) {
        
        /* test.json에 옵션 데이터가 없어 주석 처리
        List<Sourcing.ProductOption> options = new ArrayList<>(); ...
        */

        // 상대 경로인 url을 아마존 절대 경로로 변환
        String fullAmazonUrl = "https://www.amazon.com" + sourcingDTO.getUrl();

        Sourcing sourcing = Sourcing.builder()
                .sourceUrl(fullAmazonUrl) // 절대 경로로 저장
                .siteName("Amazon") // test.json에 없으므로 고정값 부여
                .productId(sourcingDTO.getAsin())
                .title(sourcingDTO.getTitle())
                .originalPrice(sourcingDTO.getPrice())
                .currency(sourcingDTO.getCurrency())
                .brand(sourcingDTO.getBrand())
                .mainImageUrl(sourcingDTO.getUrlImage())
                .descriptionImages(sourcingDTO.getImages())
                .build();

        sourcingRepository.save(sourcing);
        
    }

}