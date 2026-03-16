package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.team3Project.domain.sourcing.TranslationService.TranslationResult;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NormalizationService {

    private final SourcingRepository sourcingRepository;
    private final TranslationService translationService;
    private final CurrencyService currencyService;

    public void normalize(Long id) {
        // 상품 찾기. 
        Sourcing sourcing = sourcingRepository.findById(id)
                                              .orElseThrow(() -> new RuntimeException("상품 없음"));
        
        // KRW로 변환
        BigDecimal krwPrice = currencyService.changeKRWPrice(id);
        
        //원본 상세 이미지 번역하기.
        //원본 상세 이미지.
        List<String> descriptionImages = sourcing.getDescriptionImages();
        // 번역된 상세 이미지 저장소.
        List<String> newDescriptionImages = new ArrayList<>();
        // 상품 제목 원본.
        String originalTitle = sourcing.getTitle();

        // 1. 제목을 비동기적으로 번역합니다.
        CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(() ->
            translationService.translateText(originalTitle)  
        );

        // 2. 번역 작업이 완료될 때까지 대기 및 결과 가져오기
        String translatedTitle = titleFuture.join();


        // 상품 상세 이미지 번역하기.
        if (descriptionImages != null) {
            for (String imgUrl : descriptionImages) {
                if (imgUrl != null && imgUrl.startsWith("http")) {
                    // 각각의 상세 이미지 번역 서버로 전송하기.
                    TranslationResult resultImage = translationService.translateToKorean(translatedTitle,imgUrl);
                    newDescriptionImages.add(resultImage.resultImagePath()); // 번역된 이미지 경로 추가
                } else {
                    newDescriptionImages.add(imgUrl); // http가 아니거나 null인 경우 원본 유지
                }
            }
            // 상세 이미지 리스트 업데이트 (새로 만든 리스트로 교체)
            sourcing.setDescriptionImages(newDescriptionImages);
        }

        // 메인 이미지는 번역하지 않으니 원본 URL을 그대로 유지. DB에 올라갈 데이터 수정.
        sourcing.normalize(krwPrice, translatedTitle, sourcing.getMainImageUrl(), sourcing.getMainImageUrl());
    }
    

}
