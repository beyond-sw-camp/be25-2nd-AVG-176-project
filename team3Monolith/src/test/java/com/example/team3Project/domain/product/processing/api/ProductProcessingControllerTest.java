package com.example.team3Project.domain.product.processing.api;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.product.processing.application.ProductProcessingService;
import com.example.team3Project.domain.product.processing.application.SourcingProductMapper;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingRequest;
import com.example.team3Project.domain.product.processing.dto.ProductProcessingResultResponse;
import com.example.team3Project.domain.product.processing.dto.SourcingCompletedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductProcessingControllerTest {

    private MockMvc mockMvc;
    private ProductProcessingService productProcessingService;
    private SourcingProductMapper sourcingProductMapper;

    @BeforeEach
    void setUp() {
        productProcessingService = mock(ProductProcessingService.class);
        sourcingProductMapper = mock(SourcingProductMapper.class);

        ProductProcessingController controller =
                new ProductProcessingController(productProcessingService, sourcingProductMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("소싱 ingest API는 헤더의 사용자 식별값과 payload를 사용해 쿠팡 기준 가공 서비스를 호출한다")
    void ingestSourcingProduct_mapsPayloadAndProcessesProduct() throws Exception {
        ProductProcessingRequest processingRequest = ProductProcessingRequest.of(
                "B000OV0S84",
                "/Coca-Cola-Zero-Sugar-Fridgepack-Pack/dp/B000OV0S84",
                "Sugar Soda, 12 fl oz Cans, 12 Pack - Classic Cola Soft Drink Fridge Pack",
                "Coca-Cola Zero",
                BigDecimal.valueOf(8.42),
                "USD",
                "1/10001/desc/B000OV0S84/0.jpeg",
                List.of(
                        "1/10001/desc/B000OV0S84/0.jpeg",
                        "1/10001/desc/B000OV0S84/1.jpeg"
                )
        );

        ProductProcessingResultResponse response = new ProductProcessingResultResponse(
                false,
                null,
                "가공 상품명",
                "가공 브랜드",
                BigDecimal.valueOf(8.42),
                "USD",
                BigDecimal.valueOf(1350),
                BigDecimal.valueOf(11367),
                BigDecimal.valueOf(18000),
                BigDecimal.valueOf(3000),
                "READY"
        );

        when(sourcingProductMapper.toProcessingRequest(any(SourcingCompletedRequest.class))).thenReturn(processingRequest);
        when(productProcessingService.processProduct(1L, MarketCode.COUPANG, processingRequest)).thenReturn(response);

        mockMvc.perform(
                        post("/api/sourcing/ingest")
                                .header("X-User-Id", "1")
                                .contentType("application/json")
                                .content(validIngestPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excluded").value(false))
                .andExpect(jsonPath("$.registrationStatus").value("READY"))
                .andExpect(jsonPath("$.processedProductName").value("가공 상품명"));

        ArgumentCaptor<SourcingCompletedRequest> captor =
                ArgumentCaptor.forClass(SourcingCompletedRequest.class);

        verify(sourcingProductMapper).toProcessingRequest(captor.capture());
        verify(productProcessingService).processProduct(1L, MarketCode.COUPANG, processingRequest);

        assertEquals("B000OV0S84", captor.getValue().getAsin());
    }

    @Test
    @DisplayName("소싱 ingest API는 X-User-Id 헤더가 없으면 요청을 거절한다")
    void ingestSourcingProduct_requiresUserIdHeader() throws Exception {
        mockMvc.perform(
                        post("/api/sourcing/ingest")
                                .contentType("application/json")
                                .content(validIngestPayload()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sourcingProductMapper, productProcessingService);
    }

    private String validIngestPayload() {
        return """
                {
                  "asin": "B000OV0S84",
                  "marketCode": "COUPANG",
                  "brand": "Coca-Cola Zero",
                  "currency": "USD",
                  "price": 8.42,
                  "title": "Sugar Soda, 12 fl oz Cans, 12 Pack - Classic Cola Soft Drink Fridge Pack",
                  "url": "/Coca-Cola-Zero-Sugar-Fridgepack-Pack/dp/B000OV0S84",
                  "url_image": "1/10001/desc/B000OV0S84/0.jpeg",
                  "images": [
                    "1/10001/desc/B000OV0S84/0.jpeg",
                    "1/10001/desc/B000OV0S84/1.jpeg"
                  ],
                  "variation": [
                    {
                      "asin": "B000OV0S84",
                      "dimensions": {
                        "Flavor Name": "Coca-Cola Zero Sugar",
                        "Size": "12 fl oz (Pack of 12)"
                      },
                      "selected": true,
                      "price": 8.42,
                      "currency": "USD",
                      "stock": "In Stock",
                      "rating": 4.6,
                      "reviews_count": 9360,
                      "images": [
                        "1/10001/var/B000OV0S84/0.jpeg"
                      ]
                    }
                  ]
                }
                """;
    }
}
