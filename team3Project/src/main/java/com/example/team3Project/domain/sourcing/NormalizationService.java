package com.example.team3Project.domain.sourcing;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NormalizationService {

    private final SourcingRepository sourcingRepository;
    private final SourcingVariationRepository sourcingVariationRepository;
    private final TranslationService translationService;
    private final CurrencyService currencyService;

    // 동시 Gemini 호출 최대 2개로 제한
    private final Semaphore semaphore = new Semaphore(2);

    public void normalize(Long id) {
        // 상품 찾기.
        Sourcing sourcing = sourcingRepository.findById(id)
                                              .orElseThrow(() -> new RuntimeException("상품 없음"));
        // KRW로 변환
        BigDecimal krwPrice = currencyService.changeKRWPrice(id);

        // 1. 제목, 브랜드 비동기 번역 동기식으로 가면 오래 걸릴 예정. 2개 동시에 돌리기.
        CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(() ->
            translationService.translateText(sourcing.getTitle())
        );
        CompletableFuture<String> brandFuture = CompletableFuture.supplyAsync(() ->
            translationService.translateText(sourcing.getBrand())
        );

        String translatedTitle = titleFuture.join();
        String translatedBrand = brandFuture.join();

        // 2. 상품 상세 이미지 번역 (최대 3장, 세마포어로 동시 2개 제한)
        List<String> descriptionImages = sourcing.getDescriptionImages();
        System.out.println("descriptionImages 수: " + (descriptionImages == null ? "null" : descriptionImages.size()));
        if (descriptionImages != null) {
            List<String> limited = descriptionImages.stream().limit(3).toList();
            System.out.println("번역 대상 이미지 수: " + limited.size());
            long startTime = System.currentTimeMillis();

            List<CompletableFuture<String>> futures = limited.stream()
                .map(imgUrl -> CompletableFuture.supplyAsync(() -> {
                    if (imgUrl == null || !imgUrl.startsWith("http")) return imgUrl;
                    try {
                        semaphore.acquire();
                        try {
                            
                            String resultedImage =  translationService.translateToKorean(translatedTitle, imgUrl).resultImagePath();
                            
                            return resultedImage;
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return imgUrl;
                    }
                }))
                .toList();

            sourcing.setDescriptionImages(futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
            long endTime = System.currentTimeMillis();
            System.out.println("번역완료: "+ (endTime - startTime) + "ms");
        }

        // 메인 이미지는 번역하지 않고 원본 유지
        sourcing.normalize(krwPrice, translatedTitle, translatedBrand, sourcing.getMainImageUrl(), sourcing.getMainImageUrl());
        

        // 3. 각 variation(옵션)의 상세 이미지 번역 (최대 3장, 세마포어 공유)
        List<SourcingVariation> variations = sourcing.getVariations();
        if (variations != null) {
            for (SourcingVariation variation : variations) {
                if (variation.getImages() == null) continue;

                List<String> limited = variation.getImages().stream().limit(3).toList();

                List<CompletableFuture<String>> futures = limited.stream()
                    .map(imgUrl -> CompletableFuture.supplyAsync(() -> {
                        if (imgUrl == null || !imgUrl.startsWith("http")) return imgUrl;
                        try {
                            semaphore.acquire(); // 세마포어 사용 시작.
                            try {
                                long startTime = System.currentTimeMillis();
                                
                                String resultPath = translationService.translateToKorean(translatedTitle, imgUrl).resultImagePath();
                                long endTime = System.currentTimeMillis();
                                System.out.println("옵션 이미지 번역 완료 (" + (endTime - startTime) + "ms): " + imgUrl);
                                return resultPath;
                            } finally {
                                semaphore.release();// 세마포어 종료. 
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return imgUrl;
                        }
                    }))
                    .toList();

                variation.setImages(futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
                sourcingVariationRepository.save(variation);
            }
        }
    }

}