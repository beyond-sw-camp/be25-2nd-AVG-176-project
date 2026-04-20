package com.example.team3Project.domain.product.processing.api;

import com.example.team3Project.domain.policy.dto.ProductNameProcessingRequest;
import com.example.team3Project.domain.policy.dto.ProductNameProcessingResponse;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.product.processing.application.ProductProcessingService;
import com.example.team3Project.domain.product.processing.application.SourcingProductMapper;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingRequest;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingResultResponse;
import com.example.team3Project.domain.product.processing.dto.SourcingCompletedRequest;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductProcessingController {

    // 상품 가공 진입점을 컨트롤러에서 받고, 실제 계산과 저장은 서비스로 위임한다.
    private final ProductProcessingService productProcessingService;
    private final SourcingProductMapper sourcingProductMapper;

    @PostMapping("/products/processing/name")
    public ResponseEntity<ProductNameProcessingResponse> processProductName(
            @LoginUser User user,
            @RequestParam MarketCode marketCode,
            @Valid @RequestBody ProductNameProcessingRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ProductNameProcessingResponse response =
                productProcessingService.processProductNameResponse(user.getId(), marketCode, request.getProductName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/processing")
    public ResponseEntity<ProductProcessingResultResponse> processProduct(
            @LoginUser User user,
            @RequestParam MarketCode marketCode,
            @Valid @RequestBody ProductProcessingRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ProductProcessingResultResponse response =
                productProcessingService.processProduct(user.getId(), marketCode, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/sourcing/ingest")
    public ResponseEntity<ProductProcessingResultResponse> ingestSourcingProduct(
            // 소싱 서비스가 호출할 때는 Gateway가 붙인 X-User-Id 헤더를 기준으로 처리한다.
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SourcingCompletedRequest request
    ) {
        // 소싱 완료 payload를 가공 서비스 내부 요청 형식으로 변환한다.
        ProductProcessingRequest processingRequest = sourcingProductMapper.toProcessingRequest(request);
        // marketCode는 소싱 웹훅 루트 필드의 값을 사용한다.
        ProductProcessingResultResponse response =
                productProcessingService.processProduct(userId, request.getMarketCode(), processingRequest);

        return ResponseEntity.ok(response);
    }
}
