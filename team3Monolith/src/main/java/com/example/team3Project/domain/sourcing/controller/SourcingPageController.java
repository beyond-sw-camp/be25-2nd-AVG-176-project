package com.example.team3Project.domain.sourcing.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.integration.SourcingProcessingWebhookService;
import com.example.team3Project.domain.sourcing.service.SourcingPersistOutcome;
import com.example.team3Project.domain.sourcing.service.SourcingService;
import com.example.team3Project.support.auth.RequestUserIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingPageController {

    @Value("${fastapi.sourcing.url}")
    private String sourcingApiUrl;

    // 소싱 페이지 접근 주소 API Gateway로 나가기 위한 주소.
    @Value("${sourcing.api-gateway-public-origin:}")
    private String apiGatewayPublicOrigin;

    private final RequestUserIdResolver requestUserIdResolver;
    private final SourcingService sourcingService;
    private final SourcingProcessingWebhookService sourcingProcessingWebhookService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/auto")
    public String autoSourcingForm(Model model) {
        model.addAttribute("sourcingApiOrigin", trimOrigin(apiGatewayPublicOrigin));
        return "sourcing-test/sourcing-form";
    }

    /**
     * 소싱 자동화 파이프라인: Python 스크래핑 → 검증 → 저장+정규화 → 가공 서비스 webhook
     */
    @PostMapping("/auto")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> autoSourcing(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();

        Long userId = requestUserIdResolver.resolveForApi(request);
        if (userId == null) {
            response.put("status", "error");
            response.put("message", "인증이 필요합니다. API Gateway에서 X-User-Id 헤더를 전달해 주세요.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 1. Python 스크래핑 서버 호출
        ResponseEntity<Object> pythonResponse;
        try {
            pythonResponse = restTemplate.postForEntity(sourcingApiUrl, body, Object.class);
        } catch (Exception e) {
            log.error("Python 소싱 서버 호출 실패 userId={} err={}", userId, e.getMessage());
            response.put("status", "error");
            response.put("message", "소싱 서버 호출에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }

        if (!pythonResponse.getStatusCode().is2xxSuccessful() || pythonResponse.getBody() == null) {
            response.put("status", "error");
            response.put("message", "소싱 서버에서 데이터를 가져오지 못했습니다.");
            response.put("pythonStatus", pythonResponse.getStatusCode().value());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }

        // 2. Python 응답 래퍼에서 results 배열 + 메타데이터 추출
        // Python 응답 구조: {"status":"success","keywords":[...],"elapsed":17.7,"results":[{...상품...}],"results_json":"..."}
        List<Object> resultItems;
        Map<String, Object> pythonBody;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> pb = (Map<String, Object>) pythonResponse.getBody();
            pythonBody = pb;
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) pythonBody.get("results");
            if (items == null || items.isEmpty()) {
                log.warn("Python 응답에 results 없음 userId={} body={}", userId, pythonBody);
                response.put("status", "error");
                response.put("message", "소싱 서버가 상품 데이터를 반환하지 않았습니다.");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
            }
            resultItems = items;
        } catch (Exception e) {
            log.error("Python 응답 파싱 실패 userId={} err={}", userId, e.getMessage());
            response.put("status", "error");
            response.put("message", "소싱 서버 응답 파싱에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // 3. results 배열의 각 상품을 검증 → 저장+정규화 → webhook 처리
        List<Map<String, Object>> outcomes = new ArrayList<>();
        for (int i = 0; i < resultItems.size(); i++) {
            SourcingDTO sourcingDTO;
            try {
                sourcingDTO = objectMapper.convertValue(resultItems.get(i), SourcingDTO.class);
                log.info("상품[{}] 변환 asin={} title={} price={} url_image={}",
                        i, sourcingDTO.getAsin(), sourcingDTO.getTitle(),
                        sourcingDTO.getPrice(), sourcingDTO.getUrlImage());
            } catch (Exception e) {
                log.error("상품[{}] SourcingDTO 변환 실패 userId={} err={}", i, userId, e.getMessage());
                Map<String, Object> failItem = new HashMap<>();
                failItem.put("index", i);
                failItem.put("pipelineStatus", "error");
                failItem.put("message", "DTO 변환 실패: " + e.getMessage());
                outcomes.add(failItem);
                continue;
            }

            List<String> errors = sourcingService.validateSourcingData(sourcingDTO, userId);
            if (!errors.isEmpty()) {
                log.warn("상품[{}] 검증 실패 asin={} errors={}", i, sourcingDTO.getAsin(), errors);
                @SuppressWarnings("unchecked")
                Map<String, Object> rawItem = (Map<String, Object>) resultItems.get(i);
                Map<String, Object> failItem = new HashMap<>(rawItem); // 원본 상품 데이터 포함
                failItem.put("index", i);
                failItem.put("pipelineStatus", "validation_failed");
                failItem.put("pipelineErrors", errors);
                outcomes.add(failItem);
                continue;
            }

            SourcingPersistOutcome outcome = sourcingService.saveSourcingDataAndNormalize(sourcingDTO, userId);
            boolean ok = outcome.registrationStatus() == SourcingRegistrationStatus.NORMALIZED;

            sourcingProcessingWebhookService.notifyAfterSave(userId, outcome, sourcingDTO);
            log.info("상품[{}] 파이프라인 완료 sourcingId={} status={}", i, outcome.sourcingId(), outcome.registrationStatus());

            @SuppressWarnings("unchecked")
            Map<String, Object> rawItem = (Map<String, Object>) resultItems.get(i);
            Map<String, Object> successItem = new HashMap<>(rawItem); // 원본 상품 데이터 포함
            successItem.put("index", i);
            successItem.put("sourcingId", outcome.sourcingId());
            successItem.put("pipelineStatus", ok ? "success" : "saved_normalization_failed");
            successItem.put("normalized", ok);
            if (!ok && outcome.normalizationErrorMessage() != null) {
                successItem.put("normalizationError", outcome.normalizationErrorMessage());
            }
            outcomes.add(successItem);
        }

        // Python 메타데이터 그대로 전달 (프론트엔드 renderResult용)
        response.put("status", "done");
        response.put("userId", userId);
        response.put("keywords", pythonBody.get("keywords"));
        response.put("elapsed", pythonBody.get("elapsed"));
        response.put("item_count", pythonBody.get("item_count"));
        response.put("results_json", pythonBody.get("results_json"));
        response.put("total", resultItems.size());
        response.put("results", outcomes);
        return ResponseEntity.ok(response);
    }

    // API Gateway 공개 origin 설정값을 URL 조합에 쓰기 좋게 정리한다 (공백·끝 슬래시 제거).
    private static String trimOrigin(String s) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        return s.trim().replaceAll("/$", "");
    }
}
