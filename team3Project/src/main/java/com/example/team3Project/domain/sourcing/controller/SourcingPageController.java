package com.example.team3Project.domain.sourcing.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<Object> autoSourcing(@RequestBody Map<String, Object> body) { // 키워드랑 금지어를 전달 받음.
        // Python FastAPI로 그대로 전달하고 응답을 그대로 반환
        ResponseEntity<Object> response = restTemplate.postForEntity(
                sourcingApiUrl, // 파이썬 서버 엔드포인트 주소 
                body, // 금지어들 및 키워드
                Object.class
        );
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody()); // 파이썬 서버에서의 응답 코드랑 그에 대한 소싱 결과들.
    }
}
