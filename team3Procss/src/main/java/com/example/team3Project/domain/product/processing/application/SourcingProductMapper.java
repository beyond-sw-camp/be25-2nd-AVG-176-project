package com.example.team3Project.domain.product.processing.application;

import com.example.team3Project.domain.product.processing.dto.ProductProcessingRequest;
import com.example.team3Project.domain.product.processing.dto.SourcingCompletedRequest;
import org.springframework.stereotype.Component;

@Component
// 소싱 서비스의 완료 요청을 가공 서비스의 내부 요청 객체로 매핑한다.
// 외부 소싱 DTO와 내부 가공 DTO의 변환 책임을 분리해서 컨트롤러를 얇게 유지한다.
public class SourcingProductMapper {
    public ProductProcessingRequest toProcessingRequest(SourcingCompletedRequest request) {
        // 이미지 필드명은 URL이지만 실제 값은 MinIO objectKey다.
        // ingest API로 들어온 소싱 완료 요청을 내부 가공 요청 형식으로 정규화한다.
        return ProductProcessingRequest.of(
                request.getAsin(),
                request.getUrl(),
                request.getTitle(),
                request.getBrand(),
                request.getPrice(),
                request.getCurrency(),
                request.getUrlImage(),
                request.getImages(),
                // variation 원본도 함께 넘겨 등록 저장 단계에서 옵션/이미지 엔티티 생성에 사용한다.
                request.getVariation()
        );
    }
}
