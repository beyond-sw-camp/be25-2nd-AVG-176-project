package com.example.team3Project.domain.sourcing.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.service.SourcingPersistOutcome;
import com.example.team3Project.domain.sourcing.service.SourcingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingController {

    private final SourcingService sourcingService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId, // API Gateway에서 받은 헤더.
            @RequestBody SourcingDTO sourcingDTO) {
        Map<String, Object> response = new HashMap<>();

        // API Gateway에서 X-User-Id 헤더를 받아서 유저 맞는지 확인.
        Long userId = parseUserId(xUserId);
        if (userId == null) {
            response.put("status", "error");
            response.put("message", "인증이 필요합니다. API Gateway를 경유하거나 X-User-Id 헤더가 필요합니다.");
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
        boolean ok = outcome.registrationStatus() == SourcingRegistrationStatus.NORMALIZED;
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

        return ResponseEntity.ok(response);
    }

    // API Gateway에서 받은 헤더를 파싱해서 유저 ID를 가져옴.
    private static Long parseUserId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {// 헤더가 잘못된 값이면 null 반환.
            return null;
        }
        try {// 숫자가 아니면 바로 예외로 null 반환.
            return Long.parseLong(headerValue.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
