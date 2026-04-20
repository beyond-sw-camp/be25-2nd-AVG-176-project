package com.example.team3Project.domain.sourcing.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.integration.SourcingProcessingWebhookService;
import com.example.team3Project.domain.sourcing.service.SourcingPersistOutcome;
import com.example.team3Project.domain.sourcing.service.SourcingService;
import com.example.team3Project.support.auth.RequestUserIdResolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingController {

    private final SourcingService sourcingService;
    private final SourcingProcessingWebhookService sourcingProcessingWebhookService;
    private final RequestUserIdResolver requestUserIdResolver;

    /**
     * ASIN(= productId)와 X-User-Id 로 소싱 단건 JSON 조회. 가공 서비스는 OpenFeign + 동일 DTO를 두고 호출하면 됨.
     * Feign 클라이언트에는 반드시 RequestHeader("X-User-Id") 를 추가할 것(회원별 ASIN 구분).
     * 단건 조회
     */
    // @GetMapping("/products/{sourceProductId}")
    // public ResponseEntity<SourcingProductResponse> getSourcingProduct(
    //         HttpServletRequest request,
    //         @PathVariable("sourceProductId") String sourceProductId) {
    //     Long userId = requestUserIdResolver.resolveForApi(request);
    //     if (userId == null) {
    //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    //     }
    //     return sourcingProductQueryService.findByAsinForUser(sourceProductId, userId)
    //             .map(ResponseEntity::ok)
    //             .orElse(ResponseEntity.notFound().build());
    // }

    // 소싱한 데이터 저장. 찾은 데이터를 보고 정규화 후 DB에 저장.
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(
            HttpServletRequest request,
            @RequestBody SourcingDTO sourcingDTO) {
        Map<String, Object> response = new HashMap<>();
        // 여기서 토큰 검증 먼저 하고 그다음에 소싱한 데이터 맞는지 확인.
        Long userId = requestUserIdResolver.resolveForApi(request);
        if (userId == null) {
            response.put("status", "error");
            response.put("message", "인증이 필요합니다. API Gateway에서 X-User-Id 헤더를 전달해 주세요.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        // 소싱한 데이터 맞는지 확인.
        List<String> errors = sourcingService.validateSourcingData(sourcingDTO, userId);
        if (!errors.isEmpty()) {
            response.put("status", "error");
            response.put("message", "데이터 검증 실패");
            response.put("errors", errors);
            return ResponseEntity.ok(response);
        }
        // 정규화 시켜버리기.
        SourcingPersistOutcome outcome = sourcingService.saveSourcingDataAndNormalize(sourcingDTO, userId);
        boolean ok = outcome.registrationStatus() == SourcingRegistrationStatus.NORMALIZED; // 정규화 완료 시 true, 실패 시 false
        response.put("status", ok ? "success" : "saved_normalization_failed");
        response.put("message", ok ? "저장 및 정규화가 완료되었습니다."
                : "저장은 되었으나 정규화에 실패했습니다. 상태를 확인한 뒤 재시도하세요.");
        response.put("receivedData", sourcingDTO);
        response.put("sourcingId", outcome.sourcingId());
        response.put("registrationStatus", outcome.registrationStatus().name());
        response.put("normalized", ok);
        if (!ok && outcome.normalizationErrorMessage() != null) {
            response.put("normalizationError", outcome.normalizationErrorMessage());
        }
        response.put("userId", userId);

        // 가공 서비스에 소싱 데이터 보내기. webhook 서비스. 보내는 데이터들은 유저 아이디, 
        sourcingProcessingWebhookService.notifyAfterSave(userId, outcome, sourcingDTO);
        
        // 비동기 웹훅은 여기서는 '요청 큐잉'만 보장됨. 실제 HTTP 성공은 SourcingProcessingWebhookService 로그 확인.
        log.info("가공 웹훅 전송 요청 sourcingId={}", outcome.sourcingId());
        log.info("가공 웹훅 전송 요청 userId={}", userId);
        
        return ResponseEntity.ok(response);
    }
}
