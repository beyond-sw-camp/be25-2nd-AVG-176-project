package com.example.team3Project.domain.sourcing.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class TranslationService {
    // 설정 파일에 값이 없으면 기본값(http://localhost:8000/translateImage)을 사용하도록 수정
    @Value("${fastapi.translateImage.url:http://localhost:8000/translateImage}")
    private String fastApiUrl;

    public record TranslationResult( String localImagePath, String resultImagePath) {}

    // 파이썬 서버로 이동하기위한 코드.
    public TranslationResult translateToKorean(String text, String imageUrl) {
        // 파이썬 서버로 이동하기 전의 설정.
        RestTemplate restTemplate = new RestTemplate();

        // 파이썬 서버에 json 데이터 형식을 맞춰 보낼 예정.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 여길 통해서 엔드포인트가 원하는 데이터 형식을 맞춤. 
        Map<String,String> body = Map.of(
            "image_url", imageUrl
        );
        
        // 새로운 객체 만들어서 fastApiUrl로 보내기.
        HttpEntity<Map<String,String>> request = new HttpEntity<>(body, headers);
        Map<?, ?> response = restTemplate.postForObject(fastApiUrl, request, Map.class);

        // 만약 null 이면 통신 실패시 null 반환.
        if (response == null) {
            return new TranslationResult(imageUrl, imageUrl);
        }

        //이미지 저장 경로 가져오기.
        String localImagePath = (String) response.get("local_image_path");
        String resultImagePath = (String) response.get("result_image_path");

        // 결과값 객체를 가지면서 반환
        return new TranslationResult(
            localImagePath != null ? localImagePath : imageUrl,
            resultImagePath != null ? resultImagePath : imageUrl
        );
    }
    
        // 텍스트 변환 시작
    public String translateText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 파이썬으로 보내기 위한 설정.
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
            "text", text,
            "target_lang", "KO"
        );
        
        // 보낼 데이터를 객체로 만들어서 보낼준비. 
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // 기존 URL(.../translateImage)을 텍스트 번역용 URL(.../translate_text)로 변경
        String textTranslateUrl = fastApiUrl.replace("/translateImage", "/translate_text");

        try {
            // ?,?은 객체 타입이 정확하지 않아서 아직은 모르겟음.
            Map<?, ?> response = restTemplate.postForObject(textTranslateUrl, request, Map.class);
            if (response != null && response.containsKey("translated_text")) {
                return (String) response.get("translated_text");
            }
        } catch (Exception e) {
            System.out.println("텍스트 번역 실패: " + e.getMessage());
        }
        
        return text; // 실패 시 원본 텍스트 반환
    }

}
