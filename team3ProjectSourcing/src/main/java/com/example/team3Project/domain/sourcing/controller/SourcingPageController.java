package com.example.team3Project.domain.sourcing.controller;

import java.util.HashMap;
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

import com.example.team3Project.support.auth.RequestUserIdResolver;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingPageController {

    @Value("${fastapi.sourcing.url}")
    private String sourcingApiUrl;

    @Value("${sourcing.api-gateway-public-origin:}")
    private String apiGatewayPublicOrigin;

    private final RequestUserIdResolver requestUserIdResolver;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/auto")
    public String autoSourcingForm(Model model) {
        model.addAttribute("sourcingApiOrigin", trimOrigin(apiGatewayPublicOrigin));
        return "sourcing-test/sourcing-form";
    }

    @PostMapping("/auto")
    @ResponseBody
    public ResponseEntity<Object> autoSourcing(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = requestUserIdResolver.resolveForApi(request);
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "인증이 필요합니다. API Gateway에서 X-User-Id 헤더를 전달해 주세요.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        ResponseEntity<Object> pythonResponse = restTemplate.postForEntity(
                sourcingApiUrl,
                body,
                Object.class
        );

        return ResponseEntity.status(pythonResponse.getStatusCode()).body(pythonResponse.getBody());
    }

    private static String trimOrigin(String s) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        return s.trim().replaceAll("/$", "");
    }
}
