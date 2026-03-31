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
import com.example.team3Project.domain.sourcing.service.SourcingPersistOutcome;
import com.example.team3Project.domain.sourcing.service.SourcingService;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingController {

    private final SourcingService sourcingService;
    // DB에 저장하는 엔드포인트.
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(
            @LoginUser User loginUser,
            @RequestBody SourcingDTO sourcingDTO) {
        Map<String, Object> response = new HashMap<>();
        //로그인 되어있는지 확인.
        if (loginUser == null) {
            response.put("status", "error");
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        // 데이터 검증.
        List<String> errors = sourcingService.validateSourcingData(sourcingDTO, loginUser);
        // 검증 실패 시 에러 메시지 반환.
        if (!errors.isEmpty()) {
            response.put("status", "error");
            response.put("message", "데이터 검증 실패");
            response.put("errors", errors);
            return ResponseEntity.ok(response);
        }
        // 데이터 저장 및 정규화 또는 정규화 실패 시 에러 메시지 반환.
        SourcingPersistOutcome outcome = sourcingService.saveSourcingDataAndNormalize(sourcingDTO, loginUser);
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
        response.put("userId", loginUser.getId());

        return ResponseEntity.ok(response);
    }

}
