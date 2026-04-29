package com.example.team3Project.domain.sourcing.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.entity.SourcingVariation;
import com.example.team3Project.domain.sourcing.integration.SourcingMainImageMinioService;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;
import com.example.team3Project.domain.sourcing.repository.SourcingVariationRepository;

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
    private final ObjectProvider<SourcingMainImageMinioService> mainImageMinioUploader;

    // 동시 Gemini 호출 최대 2개로 제한 이미지 번역에 사용
    private final Semaphore semaphore = new Semaphore(2);

    public void normalize(Long id) {
        // 상품 찾기.
        Sourcing sourcing = sourcingRepository.findById(id)
                                              .orElseThrow(() -> new RuntimeException("상품 없음"));
        // KRW로 변환
        BigDecimal krwPrice = currencyService.changeKRWPrice(id);

        // 1. 제목, 브랜드 비동기 번역 동기식으로 가면 오래 걸릴 예정. 2개 동시에 돌리기.
        // 이게 CompletableFuture에 대해서 설명하자면 백그라운드의 다른 스레드에서 작업을 실행함. 쉽게 말해서 비동기 작업 진행
        CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(() ->
            translationService.translateText(sourcing.getTitle())
        );
        CompletableFuture<String> brandFuture = CompletableFuture.supplyAsync(() ->
            translationService.translateText(sourcing.getBrand())
        );

        String translatedTitle = titleFuture.join();
        String translatedBrand = brandFuture.join();

        // 2. 상품 상세 이미지 번역 — desc에서 1장만 (Gemini 비용·시간 절감, 실제 서비스에서는 전부 번역) 
        List<String> descriptionImages = sourcing.getDescriptionImages();
        System.out.println("descriptionImages 수: " + (descriptionImages == null ? "null" : descriptionImages.size()));
        if (descriptionImages != null) {
            List<String> limited = descriptionImages.stream().limit(1).toList();
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
        sourcing.normalize(krwPrice,translatedTitle, translatedBrand, sourcing.getMainImageUrl(), sourcing.getMainImageUrl());

        // 번역된 설명 이미지들을 MinIO에 업로드하고, DB 값을 MinIO 키로 교체
        mainImageMinioUploader.ifAvailable(uploader -> {
            List<String> descImgs = sourcing.getDescriptionImages();
            if (descImgs != null) {
                AtomicInteger idx = new AtomicInteger(0);
                List<String> uploaded = new ArrayList<>();
                for (String imgPath : descImgs) {
                    int i = idx.getAndIncrement();
                    uploaded.add(
                        uploader.uploadTranslatedImage(
                            // 원본 이미지 주소를 MinIO에 업로드.
                                sourcing.getUserId(), sourcing.getId(), imgPath, "desc/" + i)
                            .orElse(imgPath)
                    );
                }
                sourcing.setDescriptionImages(uploaded);
            }
        });

        // 3. 각 variation 처리 — 텍스트/재고는 즉시, 이미지는 모든 variation 것을 한꺼번에 비동기 제출
        List<SourcingVariation> variations = sourcingVariationRepository.findBySourcingId(id);
        if (variations != null && !variations.isEmpty()) {

            // 3-1. dimension 번역 + 재고 텍스트 처리 (가벼운 작업, 먼저 일괄 처리)
            for (SourcingVariation variation : variations) {
                if (variation.getDimensions() != null) {
                    Map<String, String> translatedDimensions = variation.getDimensions().entrySet().stream()
                        .collect(Collectors.toMap(
                            e -> translationService.translateText(e.getKey()),
                            e -> translationService.translateText(e.getValue())
                        ));
                    variation.setDimensions(translatedDimensions);
                }

                String varitionStock = variation.getStock();
                if (varitionStock != null) {
                    if (varitionStock.contains("In Stock")) {
                        variation.setStock("재고 있음");
                    } else if (varitionStock.contains("Currently unavailable")) {
                        variation.setStock("일시 품절");
                    }
                }
            }

            // 3-2. variation 이미지 — 옵션 종류와 무관하게 전체에서 1장만 번역 (세마포어가 동시성 제한)
            final int MAX_VARIATION_IMAGES = 2;
            record ImageTask(SourcingVariation variation, int index, CompletableFuture<String> future) {}
            List<ImageTask> allTasks = new ArrayList<>();

            long imgStartTime = System.currentTimeMillis();
            for (SourcingVariation variation : variations) {
                if (allTasks.size() >= MAX_VARIATION_IMAGES) break;
                if (variation.getImages() == null) continue;
                List<String> limited = variation.getImages().stream()
                    .limit(Math.max(0, MAX_VARIATION_IMAGES - allTasks.size()))
                    .toList();

                for (int i = 0; i < limited.size(); i++) {
                    final String imgUrl = limited.get(i);
                    final int idx = i;
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        if (imgUrl == null || !imgUrl.startsWith("http")) return imgUrl;
                        try {
                            semaphore.acquire();
                            try {
                                long t0 = System.currentTimeMillis();
                                String resultPath = translationService.translateToKorean(translatedTitle, imgUrl).resultImagePath();
                                System.out.println("옵션 이미지 번역 완료 (" + (System.currentTimeMillis() - t0) + "ms): " + imgUrl);
                                return resultPath;
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return imgUrl;
                        }
                    });
                    allTasks.add(new ImageTask(variation, idx, future));
                }
            }

            // 3-3. 전부 완료 대기
            CompletableFuture.allOf(
                allTasks.stream().map(ImageTask::future).toArray(CompletableFuture[]::new)
            ).join();

            System.out.println("전체 옵션 이미지 번역 완료: " + (System.currentTimeMillis() - imgStartTime) + "ms, 총 " + allTasks.size() + "장");

            // 3-4. 결과를 각 variation에 반영
            Map<Long, List<String>> resultsByVariation = new java.util.LinkedHashMap<>();
            for (ImageTask task : allTasks) {
                resultsByVariation
                    .computeIfAbsent(task.variation().getId(), k -> new ArrayList<>())
                    .add(task.future().join());
            }

            for (SourcingVariation variation : variations) {
                List<String> translatedImgs = resultsByVariation.get(variation.getId());
                if (translatedImgs == null) continue;

                mainImageMinioUploader.ifAvailable(uploader -> {
                    AtomicInteger vi = new AtomicInteger(0);
                    List<String> uploaded = new ArrayList<>();
                    String varAsin = variation.getAsin() != null ? variation.getAsin() : "unknown";
                    for (String imgPath : translatedImgs) {
                        int i = vi.getAndIncrement();
                        uploaded.add(
                            uploader.uploadTranslatedImage(
                                    sourcing.getUserId(), sourcing.getId(), imgPath,
                                    "var/" + varAsin + "/" + i)
                                .orElse(imgPath)
                        );
                    }
                    variation.setImages(uploaded);
                });

                sourcingVariationRepository.save(variation);
            }
        }

        sourcing.setRegistrationStatus(SourcingRegistrationStatus.NORMALIZED);
        sourcingRepository.save(sourcing);
    }

}