package com.example.team3Project.domain.sourcing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SourcingService { 

    // mariaDB에 저장하기 위한 하나의 객체.
    private final SourcingRepository sourcingRepository;

    // 일단 json 파일이 제대로 원하는 값들이 있는지 확인.
    public List<String> validateSourcingData(SourcingDTO sourcingDTO) {
        List<String> errors = new ArrayList<>();

       // json 파일 확인 해보는데 만약 뭔가가 누락이 되었다면, 그 즉시 누락 되었다고 나오기.
        if (!StringUtils.hasText(sourcingDTO.getSourceUrl())) {
            errors.add("sourceUrl가 누락 되었습니다.");
        }
        if (!StringUtils.hasText(sourcingDTO.getProductId())) {
            errors.add("productId가 누락 되었습니다.");
        }
        // 옵션 및 다양한 data 누락 되었는지 확인하기.
        if (sourcingDTO.getData() == null) {
            errors.add("data의 정보가 누락 되었습니다.");
            // 여기에 없으면 바로 return 하면 되니까.
            return errors;
        }
        
        // 데이터 안의 정보들 있는지 확인하기 위해 미리 사용
        SourcingDTO.ProductData productData = sourcingDTO.getData();

        if (!StringUtils.hasText(productData.getTitle())) {
            errors.add("title이 누락 되었습니다.");
        }
        if (productData.getOriginalPrice() == null) {
            errors.add("originalPrice가 누락 되었습니다.");
        }
        if (!StringUtils.hasText(productData.getCurrency())) {
            errors.add("currency가 누락 되었습니다.");
        }

        if (!StringUtils.hasText(productData.getBrand())){
            errors.add("brand가 누락 되었습니다.");
        }

        if (!StringUtils.hasText(productData.getMainImageUrl())) {
            errors.add("mainImageUrl이 누락 되었습니다.");
        }

        // 데이터 안에 옵션 없는 경우 작성.
        if (productData.getOptions() == null || productData.getOptions().isEmpty()) {
            errors.add("options가 누락 되었습니다.");
        } else {
            for (SourcingDTO.Option option : productData.getOptions()) {
                if (!StringUtils.hasText(option.getName())) {
                    errors.add("option의 name이 누락 되었습니다.");
                }
                if (option.getValues() == null || option.getValues().isEmpty()) {
                    errors.add("option의 values가 누락 되었습니다.");
                }
            }
        }

        return errors;
    
}

    @Transactional
    public void saveSourcingData(SourcingDTO sourcingDTO) {
        Sourcing sourcing = Sourcing.builder()
                .sourceUrl(sourcingDTO.getSourceUrl())
                .siteName(sourcingDTO.getSiteName())
                .productId(sourcingDTO.getProductId())
                .collectedAt(sourcingDTO.getCollectedAt())
                .title(sourcingDTO.getData().getTitle())
                .originalPrice(sourcingDTO.getData().getOriginalPrice())
                .currency(sourcingDTO.getData().getCurrency())
                .brand(sourcingDTO.getData().getBrand())
                .mainImageUrl(sourcingDTO.getData().getMainImageUrl())
                .stockStatus(sourcingDTO.getData().getStockStatus())
                .build();

        sourcingRepository.save(sourcing);
        
    }

}