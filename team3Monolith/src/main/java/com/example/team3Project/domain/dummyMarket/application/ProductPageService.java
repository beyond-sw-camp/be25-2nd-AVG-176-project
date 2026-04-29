package com.example.team3Project.domain.dummyMarket.application;

import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProduct;
import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProductImage;
import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProductImage.ImageType;
import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProductOption;
import com.example.team3Project.domain.dummyMarket.dao.DummyMarketCoupangProductRepository;
import com.example.team3Project.domain.dummyMarket.dto.ProductListPageDto;
import com.example.team3Project.domain.dummyMarket.dto.ProductPageDto;
import com.example.team3Project.domain.dummyMarket.dto.ProductPageDto.OptionDto;
import com.example.team3Project.domain.dummyMarket.dto.ProductPageDto.ProductSummaryDto;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPageService {

    private final DummyMarketCoupangProductRepository productRepository;
    private final SourcingRepository sourcingRepository;
    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket:sourcing-images}")
    private String defaultBucketName;

    @Transactional(readOnly = true)
    public ProductPageDto getProductPage(Long productId) {
        return getProductPage(productId, 0, 12);
    }

    @Transactional(readOnly = true)
    public ProductListPageDto getProductListPage(int page, int size) {
        // 전체 상품 목록은 모든 판매자의 상품을 대상으로 페이지네이션한다.
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 60);
        Page<DummyCoupangProduct> products = productRepository.findAllByOrderByIdAsc(
                PageRequest.of(safePage, safeSize)
        );

        return ProductListPageDto.builder()
                .products(products.stream()
                        .map(product -> ProductListPageDto.ProductSummaryDto.builder()
                                .productId(product.getId())
                                .userId(product.getUserId())
                                .productName(product.getProductName())
                                .mainImageUrl(resolveRawImageUrl(product.getMainImageUrl()))
                                .salePrice(roundPrice(product.getSalePrice()))
                                .build())
                        .toList())
                .page(products.getNumber())
                .size(products.getSize())
                .totalPages(products.getTotalPages())
                .totalElements(products.getTotalElements())
                .hasPrevious(products.hasPrevious())
                .hasNext(products.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductPageDto getProductPage(Long productId, int sidebarPage, int sidebarSize) {
        DummyCoupangProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<DummyCoupangProductImage> images = product.getImages();
        List<DummyCoupangProductOption> options = product.getOptions();

        List<String> mainImageUrls = images.stream()
                .filter(img -> img.getImageType() == ImageType.MAIN)
                .map(this::resolveImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toList());

        List<String> descriptionImageUrls = images.stream()
                .filter(img -> img.getImageType() == ImageType.DESCRIPTION)
                .map(this::resolveImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toList());

        // DESCRIPTION 이미지가 없으면 sourcing_description_images 에서 폴백
        if (descriptionImageUrls.isEmpty() && product.getSourceProductId() != null) {
            descriptionImageUrls = sourcingRepository.findByProductId(product.getSourceProductId())
                    .stream()
                    .flatMap(s -> s.getDescriptionImages().stream())
                    .map(this::resolveRawImageUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .distinct()
                    .toList();
        }

        Map<String, String> optionImageMap = images.stream()
                .filter(img -> img.getImageType() == ImageType.OPTION && img.getOptionAsin() != null)
                .map(img -> new AbstractMap.SimpleEntry<>(img.getOptionAsin(), resolveImageUrl(img)))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first
                ));

        if (mainImageUrls.isEmpty() && product.getMainImageUrl() != null) {
            String mainImageUrl = resolveRawImageUrl(product.getMainImageUrl());
            if (mainImageUrl != null && !mainImageUrl.isBlank()) {
                mainImageUrls.add(mainImageUrl);
            }
        }

        List<OptionDto> optionDtos = options.stream()
                .map(opt -> OptionDto.builder()
                        .optionId(opt.getId())
                        .optionAsin(opt.getOptionAsin())
                        .optionDimensions(parseOptionDimensions(opt.getOptionDimensions()))
                        .price(roundPrice(opt.getSalePrice()))
                        .selected(Boolean.TRUE.equals(opt.getSelected()))
                        .stock(opt.getStock())
                        .currency(opt.getCurrency())
                        .imageUrl(optionImageMap.getOrDefault(opt.getOptionAsin(), ""))
                        .build())
                .collect(Collectors.toList());

        // 상세 페이지 사이드바는 현재 상품의 판매자(userId) 상품만 페이지네이션한다.
        int safeSidebarPage = Math.max(sidebarPage, 0);
        int safeSidebarSize = Math.min(Math.max(sidebarSize, 1), 50);
        Page<DummyCoupangProduct> sidebarProducts = productRepository.findByUserIdOrderByIdAsc(
                product.getUserId(),
                PageRequest.of(safeSidebarPage, safeSidebarSize)
        );

        List<ProductSummaryDto> allProducts = sidebarProducts.stream()
                .map(p -> ProductSummaryDto.builder()
                        .productId(p.getId())
                        .productName(p.getProductName())
                        .mainImageUrl(resolveRawImageUrl(p.getMainImageUrl()))
                        .salePrice(roundPrice(p.getSalePrice()))
                        .build())
                .collect(Collectors.toList());

        return ProductPageDto.builder()
                .productId(product.getId())
                .userId(product.getUserId())
                .productName(product.getProductName())
                .brand(product.getBrand())
                .salePrice(roundPrice(product.getSalePrice()))
                .originalPrice(product.getOriginalPrice())
                .shippingFee(product.getShippingFee())
                .mainImageUrls(mainImageUrls)
                .descriptionImageUrls(descriptionImageUrls)
                .options(optionDtos)
                .allProducts(allProducts)
                .sidebarPage(sidebarProducts.getNumber())
                .sidebarSize(sidebarProducts.getSize())
                .sidebarTotalPages(sidebarProducts.getTotalPages())
                .sidebarTotalElements(sidebarProducts.getTotalElements())
                .sidebarHasPrevious(sidebarProducts.hasPrevious())
                .sidebarHasNext(sidebarProducts.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public Long getFirstProductId() {
        return productRepository.findAll().stream()
                .findFirst()
                .map(DummyCoupangProduct::getId)
                .orElseThrow(() -> new IllegalStateException("No products are registered."));
    }

    private String resolveImageUrl(DummyCoupangProductImage image) {
        String objectKey  = image.getObjectKey();
        String bucketName = image.getBucketName();
        String imageUrl   = image.getImageUrl();

        // objectKey가 있고 http URL이 아니면 MinIO presigned URL로 시도
        if (objectKey != null && !objectKey.isBlank() && !objectKey.startsWith("http")) {
            String bucket = (bucketName != null && !bucketName.isBlank()) ? bucketName : "sourcing-images";
            try {
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(objectKey)
                                .expiry(1, TimeUnit.DAYS)
                                .build()
                );
            } catch (Exception e) {
                log.warn("MinIO presigned URL 실패 (bucket={}, key={}). direct URL 폴백", bucket, objectKey, e);
                // MinIO 실패 시 imageUrl이 있으면 사용, 없으면 직접 경로
                return (imageUrl != null && !imageUrl.isBlank()) ? imageUrl
                        : (minioUrl + "/" + bucket + "/" + objectKey);
            }
        }

        return resolveRawImageUrl(imageUrl);
    }

    private String resolveRawImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String trimmedUrl = rawUrl.trim();
        if (trimmedUrl.startsWith("http://")
                || trimmedUrl.startsWith("https://")
                || trimmedUrl.startsWith("data:")) {
            return trimmedUrl;
        }

        String objectKey = trimmedUrl.startsWith("/") ? trimmedUrl.substring(1) : trimmedUrl;
        String bucket = defaultBucketName;

        if (objectKey.startsWith(bucket + "/")) {
            objectKey = objectKey.substring(bucket.length() + 1);
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(1, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.warn("MinIO presigned URL failed for raw image (bucket={}, key={}). Falling back to direct URL", bucket, objectKey, e);
            return minioUrl + "/" + bucket + "/" + objectKey;
        }
    }

    private String parseOptionDimensions(String dimensions) {
        if (dimensions == null || dimensions.isBlank()) {
            return "";
        }

        try {
            String cleaned = dimensions.replaceAll("[{}\"]", "");
            return cleaned.replace(",", " / ");
        } catch (Exception e) {
            return dimensions;
        }
    }

    private BigDecimal roundPrice(BigDecimal price) {
        return price == null ? null : price.setScale(0, RoundingMode.HALF_UP);
    }
}
