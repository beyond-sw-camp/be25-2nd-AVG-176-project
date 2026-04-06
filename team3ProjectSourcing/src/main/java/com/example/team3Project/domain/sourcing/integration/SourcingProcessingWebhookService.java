package com.example.team3Project.domain.sourcing.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.SourcingRegistrationStatus;
import com.example.team3Project.domain.sourcing.service.SourcingPersistOutcome;

import lombok.extern.slf4j.Slf4j;

/**
 * DB 저장·정규화 직후 가공 서버로 소싱 JSON을 POST합니다.
 * URL이 비어 있으면 아무 것도 하지 않습니다. 호출은 비동기라 업로드 API 응답은 지연되지 않습니다.
 */
@Service
@Slf4j
public class SourcingProcessingWebhookService {

    // 가공 서비스 수신 URL. 저장,정규화 직후 비동기로 JSON 전송.
    @Value("${sourcing.processing.webhook-url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 소싱 데이터 저장 후 가공 서비스에 소싱 데이터 보내기.
    public void notifyAfterSave(Long userId, SourcingPersistOutcome outcome, SourcingDTO sourcingDTO) {
        if (!StringUtils.hasText(webhookUrl)) {
            log.debug("가공 웹훅 생략: sourcing.processing.webhook-url 미설정 sourcingId={}", outcome.sourcingId());
            return;
        }
        log.info("가공 웹훅 비동기 전송 큐잉 sourcingId={} userId={}", outcome.sourcingId(), userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "SOURCING_SAVED");
        body.put("userId", userId);
        body.put("sourcingId", outcome.sourcingId());
        body.put("registrationStatus", outcome.registrationStatus().name());
        body.put("normalized", outcome.registrationStatus() == SourcingRegistrationStatus.NORMALIZED);
        if (outcome.normalizationErrorMessage() != null) {
            body.put("normalizationError", outcome.normalizationErrorMessage());
        }
        body.put("sourcing", sourcingDTO);

        CompletableFuture.runAsync(() -> send(userId, body));
    }

    private void send(Long userId, Map<String, Object> body) {
        Object sourcingId = body.get("sourcingId");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> res = restTemplate.postForEntity(webhookUrl.trim(), entity, String.class);
            log.info("가공 웹훅 HTTP 성공 sourcingId={} userId={} status={} url={}",
                    sourcingId, userId, res.getStatusCode(), webhookUrl.trim());
        } catch (RestClientException ex) {
            log.warn("가공 웹훅 HTTP 실패 sourcingId={} userId={} url={} err={}",
                    sourcingId, userId, webhookUrl, ex.getMessage());
        } catch (Exception ex) {
            log.error("가공 웹훅 전송 중 예외 sourcingId={} userId={}", sourcingId, userId, ex);
        }
    }
}
