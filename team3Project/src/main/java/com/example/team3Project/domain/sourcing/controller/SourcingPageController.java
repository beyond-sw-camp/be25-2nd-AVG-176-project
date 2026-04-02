package com.example.team3Project.domain.sourcing.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/sourcing")
public class SourcingPageController {

    @Value("${fastapi.sourcing.url}")
    private String sourcingApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/auto")
    public String autoSourcingForm() {
        return "sourcing-test/sourcing-form";
    } 
    
    // 소싱 버튼 누르면 소싱 시작.
    @PostMapping("/auto")
    @ResponseBody
    public ResponseEntity<Object> autoSourcing(
        @RequestHeader(value = "X-User-Id", required = false) String xUserId, // API Gateway에서 받은 헤더.
        @RequestBody Map<String, Object> body) { // 키워드랑 금지어를 전달 받음.
        // Python FastAPI로 그대로 전달하고 응답을 그대로 반환
        Long userId = parseUserId(xUserId);
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "인증이 필요합니다. API Gateway를 경유하거나 X-User-Id 헤더가 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        ResponseEntity<Object> pythonResponse = restTemplate.postForEntity(
                sourcingApiUrl,
                body,
                Object.class
        );

        return ResponseEntity.status(pythonResponse.getStatusCode()).body(pythonResponse.getBody());
    }

    private static Long parseUserId(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(xUserId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
