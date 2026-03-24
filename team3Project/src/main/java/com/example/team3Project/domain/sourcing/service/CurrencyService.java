package com.example.team3Project.domain.sourcing.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.team3Project.domain.sourcing.entity.Sourcing;
import com.example.team3Project.domain.sourcing.repository.SourcingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// 환율 가져와서 KRW로 바꾸는 방식.
@Service
public class CurrencyService {
    
    private final SourcingRepository sourcingRepository;
    @Value("${koreaexim.api.authkey}")
        private String authKey;

    public CurrencyService(SourcingRepository sourcingRepository) {
        this.sourcingRepository = sourcingRepository;
    }
    

    // 여기는 환율 가져오기.
    private String getCurrency() {
        // 얘는 곧 지울 예정.
        
        // 가장 최근의 영업일(평일) 날짜를 동적으로 계산
        // String searchDate = getLastesWeekday();
        // 테스트용 환율 나중에 일일 환율로 바꿀 예정. getLastesWeekday 이거 사용하면 그만.
        String searchDate = "20260311";
        URI uri = UriComponentsBuilder
                    .fromUriString("https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON")
                    .queryParam("authkey", authKey) // API인증키 파라미터 더해주기.
                    .queryParam("searchdate", searchDate) // 날짜 파라미터 더해주기.
                    .queryParam("data", "AP01") // AP01은 환율 정보
                    .encode()
                    .build()
                    .toUri();

        RestTemplate restTemplate = new RestTemplate();
        String gettingJson = restTemplate.getForObject(uri, String.class);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> data = objectMapper.readValue(gettingJson, new TypeReference<List<Map<String, String>>>() {});

            return data.stream()
                       .filter(map -> "USD".equals(map.get("cur_unit")))
                       .findFirst()
                       .map(map -> map.get("deal_bas_r"))
                       .orElse("환율 정보가 없음.");

        } catch (Exception e) {
            return "에러 발생: " + e.getMessage();
        }

}   
    //최근 날짜 주말 가져오기. 주말이면 -1 -2 해서 요일 찾는 방식.
    //일단 오전 00시면 또 환율 정보가 없음. 이거 기준을 10시로 잡고 해야할듯
    private String getLastesWeekday() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // 주말이면 금요일로 바꾸기.
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            today = today.minusDays(1);
        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
            today = today.minusDays(2);
        }

        // 이 아래에다가 공휴일이면 그 전날이나 최근의 환율 적용하게 하기.


        return today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // 환율 BigDecimal로 반환 (variation 등 직접 변환용)
    public BigDecimal getExchangeRate() {
        String curCurrency = getCurrency();
        if (curCurrency == null || !curCurrency.matches("^[0-9,.]+$")) {
            throw new RuntimeException("유효하지 않은 환율 데이터: " + curCurrency);
        }
        return new BigDecimal(curCurrency.replace(",", ""));
    }

    //DB에서 price 가져와서 가져온 환율 적용하기.
    public BigDecimal changeKRWPrice(Long id) {
        String curCurrency = getCurrency();
        

        //DB에서 그냥 가격 가져오기.
        Sourcing sourcing = sourcingRepository.findById(id)
                                              .orElseThrow(() -> new RuntimeException("상품 없음"));
        
        BigDecimal usdPrice = sourcing.getOriginalPrice();

        if (curCurrency == null || !curCurrency.matches("^[0-9,.]+$")) {
            throw new RuntimeException("유효하지 않은 데이터입니다." + curCurrency);
        }

        BigDecimal KRWPrice = usdPrice.multiply(new BigDecimal(curCurrency.replace(",","")));
        
        return KRWPrice;
    }

}
