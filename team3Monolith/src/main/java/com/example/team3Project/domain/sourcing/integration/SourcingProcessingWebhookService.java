package com.example.team3Project.domain.sourcing.integration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.team3Project.domain.sourcing.DTO.SourcingDTO;
import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.entity.SourcingVariation;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;
import com.example.team3Project.domain.sourcing.repository.SourcingVariationRepository;
import com.example.team3Project.domain.sourcing.service.SourcingPersistOutcome;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 가공 서비스에 전달하는 하나의 서비스 클래스.
 * DB 저장·정규화 직후 가공 서버로 소싱 JSON을 POST합니다.
 * URL이 비어 있으면 아무 것도 하지 않습니다. 호출은 비동기라 업로드 API 응답은 지연되지 않습니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SourcingProcessingWebhookService {

    private final SourcingRepository sourcingRepository;
    private final SourcingVariationRepository sourcingVariationRepository;
    // 각각의 설정값들.
    // 가공 서비스에 전달하는 웹훅 주소.
    @Value("${sourcing.processing.webhook-url:}")
    private String webhookUrl;
    // 요청 바디에 넣는 marketCode의 기본값입니다(기본 "US"). 가공 API가 기대하는 마켓 코드와 맞아야 합니다.
    @Value("${sourcing.processing.default-market-code:COUPANG}")
    private String defaultMarketCode;
    // 웹훅 연결 타임아웃 시간(기본 15초). 가공 API 연결 실패 시 재시도.
    @Value("${sourcing.processing.webhook.connect-timeout-ms:15000}")
    private int connectTimeoutMs;
    //연결 후 응답 바디를 다 읽을 때까지 기다리는 시간(ms). 가공 서버가 처리가 길면 여기서 타임아웃 날 수 있습니다.
    @Value("${sourcing.processing.webhook.read-timeout-ms:120000}")
    private int readTimeoutMs;
    // 전송 실패 시 몇 번까지 재시도할지
    @Value("${sourcing.processing.webhook.max-retries:3}")
    private int maxRetries;
    // 가공 서버에 전송하기 위한 템플릿.
    private RestTemplate webhookRestTemplate;

    @PostConstruct
    // RestTemplate 초기화 
    void initWebhookRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        webhookRestTemplate = new RestTemplate(factory);
    }

    // 
    public void notifyAfterSave(Long userId, SourcingPersistOutcome outcome, SourcingDTO sourcingDTO) {
        if (!StringUtils.hasText(webhookUrl)) { // 주소 없는 경우
            log.debug("가공 웹훅 생략: sourcing.processing.webhook-url 미설정 sourcingId={}", outcome.sourcingId());
            return;
        }
        log.info("가공 웹훅 비동기 전송 큐잉 sourcingId={} userId={}", outcome.sourcingId(), userId);
        // 결과의 소싱 아이디.
        Long sourcingId = outcome.sourcingId();
        // 정규화된 DB가 있으면 description·variation 이미지는 MinIO 키(및 번역된 텍스트)를 사용합니다.
        Sourcing normalized = sourcingRepository.findById(sourcingId).orElse(null);
        // 가공 서비스 ingest는 루트에 상품 필드가 와야 하며, {@code sourcing} 래퍼를 쓰면 바인딩이 전부 null이 됩니다.
        Map<String, Object> body = buildIngestBody(sourcingDTO, normalized);
        // 가공 서비스에 전송하기 위한 비동기 작업.
        CompletableFuture.runAsync(() -> send(userId, sourcingId, body));
    }

    /**
     * 가공 서비스 ingest는 루트에 상품 필드가 와야 하며, {@code sourcing} 래퍼를 쓰면 바인딩이 전부 null이 됩니다.
     * 정규화된 DB가 있으면 description·variation 이미지는 MinIO 키(및 번역된 텍스트)를 사용합니다.
     * 즉 Json파일의 데이터를 풀어서 가공 서비스에 전송하기 위한 함수.
     */
    private Map<String, Object> buildIngestBody(SourcingDTO d, Sourcing normalized) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("marketCode", defaultMarketCode);
        body.put("asin", d.getAsin());

        // 번역된 제목/브랜드가 있으면 사용, 없으면 원본 영어
        body.put("title", normalized != null && normalized.getTranslatedTitle() != null
                ? normalized.getTranslatedTitle() : d.getTitle());
        body.put("brand", normalized != null && normalized.getTranslatedBrand() != null
                ? normalized.getTranslatedBrand() : d.getBrand());

        body.put("price", effectivePrice(d));
        body.put("currency", d.getCurrency());

        body.put("url", resolveAmazonProductUrl(d.getUrl()));
        body.put("url_image", d.getUrlImage());

        if (normalized != null && normalized.getDescriptionImages() != null) {
            body.put("images", normalized.getDescriptionImages());
        } else {
            body.put("images", d.getImages());
        }

        body.put("variation", buildVariationPayload(d, normalized));
        return body;
    }

    /**
     * DTO 순서를 유지하면서, ASIN이 일치하는 DB variation이 있으면 번역·MinIO 반영 값을 사용합니다.
     */
    private List<Map<String, Object>> buildVariationPayload(SourcingDTO d, Sourcing normalized) {
        List<SourcingDTO.VariationDTO> dtoList = d.getVariation();
        if (dtoList == null) {
            return null;
        }
        if (dtoList.isEmpty()) {
            return List.of();
        }
        if (normalized == null || normalized.getId() == null) {
            return dtoList.stream().map(SourcingProcessingWebhookService::variationDtoToMap).toList();
        }

        List<SourcingVariation> dbVars = sourcingVariationRepository.findBySourcingId(normalized.getId());
        Map<String, SourcingVariation> byAsin = dbVars.stream()
                .filter(v -> v.getAsin() != null)
                .collect(Collectors.toMap(SourcingVariation::getAsin, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> out = new ArrayList<>(dtoList.size());
        for (SourcingDTO.VariationDTO dto : dtoList) {
            SourcingVariation ent = dto.getAsin() != null ? byAsin.get(dto.getAsin()) : null;
            if (ent != null) {
                out.add(mergeVariationFromEntity(dto, ent));
            } else {
                out.add(variationDtoToMap(dto));
            }
        }
        return out;
    }

    // variation 행 없음(또는 상품 자체를 못 읽음) 그때 사용하는 매서드.
    private static Map<String, Object> mergeVariationFromEntity(SourcingDTO.VariationDTO dto, SourcingVariation ent) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("asin", ent.getAsin() != null ? ent.getAsin() : dto.getAsin());
        m.put("dimensions", ent.getDimensions() != null ? ent.getDimensions() : dto.getDimensions());
        m.put("selected", ent.isSelected());
        m.put("price", dto.getPrice() != null ? dto.getPrice() : ent.getPrice());
        m.put("currency", dto.getCurrency() != null ? dto.getCurrency() : ent.getCurrency());
        m.put("stock", ent.getStock() != null ? ent.getStock() : dto.getStock());
        m.put("rating", ent.getRating() != null ? ent.getRating() : dto.getRating());
        m.put("reviews_count", ent.getReviewsCount() != null ? ent.getReviewsCount() : dto.getReviewsCount());
        List<String> imgs = ent.getImages();
        if (imgs != null && !imgs.isEmpty()) {
            m.put("images", imgs);
        } else {
            m.put("images", dto.getImages());
        }
        return m;
    }
    // variation 행 있음(또는 상품 자체를 읽음) 그때 사용하는 매서드.
    private static Map<String, Object> variationDtoToMap(SourcingDTO.VariationDTO dto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("asin", dto.getAsin());
        m.put("dimensions", dto.getDimensions());
        m.put("selected", dto.isSelected());
        m.put("price", dto.getPrice());
        m.put("currency", dto.getCurrency());
        m.put("stock", dto.getStock());
        m.put("rating", dto.getRating());
        m.put("reviews_count", dto.getReviewsCount());
        m.put("images", dto.getImages());
        return m;
    }

    // 상품 가격 계산 함수.
    private static BigDecimal effectivePrice(SourcingDTO sourcingDTO) {
        BigDecimal p = sourcingDTO.getPrice();
        if (p != null) {
            return p;
        }
        if (sourcingDTO.getVariation() == null) {
            return null;
        }
        return sourcingDTO.getVariation().stream()
                .map(SourcingDTO.VariationDTO::getPrice)
                .filter(x -> x != null && x.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    // 아마존 상품 주소 정리 함수.
    private static String resolveAmazonProductUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String t = url.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) {
            return t;
        }
        return "https://www.amazon.com" + t;
    }

    // 데이터 변경 완료 후 가공 서비스에 전송하는 함수.
    private void send(Long userId, Long sourcingId, Map<String, Object> body) {
        String url = webhookUrl.trim();
        long backoffMs = 2000; // 2초 뒤에 재시도.
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-User-Id", String.valueOf(userId)); //X-User-Id에서 유저 아이디 설정.
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                // body 내용을 그대로 참조하여 서버에 보내버리기.
                ResponseEntity<String> res = webhookRestTemplate.postForEntity(url, entity, String.class);
                log.info("가공 웹훅 HTTP 성공 sourcingId={} userId={} status={} url={} attempt={}",
                        sourcingId, userId, res.getStatusCode(), url, attempt);
                return;
            } catch (RestClientException ex) {
                log.warn("가공 웹훅 HTTP 실패 sourcingId={} userId={} url={} attempt={}/{} err={}",
                        sourcingId, userId, url, attempt, maxRetries, ex.getMessage());
                if (attempt >= maxRetries) {
                    return; // 최대 재시도 횟수 초과 시 종료.
                }
                try {
                    Thread.sleep(backoffMs); // 2초 뒤에 재시도.
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("가공 웹훅 재시도 대기 중 인터럽트 sourcingId={}", sourcingId);
                    return;
                }
                backoffMs *= 2;
            } catch (Exception ex) {
                log.error("가공 웹훅 전송 중 예외 sourcingId={} userId={}", sourcingId, userId, ex);
                return;
            }
        }
    }
}
