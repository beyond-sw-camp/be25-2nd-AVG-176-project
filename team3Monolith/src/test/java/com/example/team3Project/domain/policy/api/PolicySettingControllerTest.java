package com.example.team3Project.domain.policy.api;

import com.example.team3Project.domain.policy.application.PolicySettingService;
import com.example.team3Project.domain.policy.dto.PolicySettingResponse;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.PriceRoundingUnit;
import com.example.team3Project.domain.policy.entity.ShippingFeeType;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicySettingControllerTest {

    private MockMvc mockMvc;
    private PolicySettingService policySettingService;

    @BeforeEach
    void setUp() {
        policySettingService = mock(PolicySettingService.class);
        PolicySettingController controller = new PolicySettingController(policySettingService);

        // 정책 설정 컨트롤러가 로그인 사용자 기준으로 동작하는지만 빠르게 검증한다.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(createUser(1L)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("정책 설정 조회 API는 로그인 사용자 기준으로 조회한다")
    void getPolicySetting_usesLoginUser() throws Exception {
        when(policySettingService.getPolicySetting(1L, MarketCode.COUPANG))
                .thenReturn(policySettingResponse());

        mockMvc.perform(
                        get("/policies/settings")
                                .param("marketCode", "COUPANG")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.marketCode").value("COUPANG"))
                .andExpect(jsonPath("$.exchangeRate").value(1350));

        verify(policySettingService).getPolicySetting(1L, MarketCode.COUPANG);
    }

    @Test
    @DisplayName("정책 설정 저장 API는 로그인 사용자 기준으로 저장한다")
    void upsertPolicySetting_usesLoginUser() throws Exception {
        when(policySettingService.upsertPolicySetting(any(), any(), any()))
                .thenReturn(policySettingResponse());

        mockMvc.perform(
                        put("/policies/settings")
                                .param("marketCode", "COUPANG")
                                .contentType("application/json")
                                .content(validPolicySettingRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.marketCode").value("COUPANG"))
                .andExpect(jsonPath("$.targetMarginRate").value(35))
                .andExpect(jsonPath("$.roundingUnit").value("HUNDRED_WON"))
                .andExpect(jsonPath("$.shippingFeeType").value("PAID_SHIPPING"));

        verify(policySettingService).upsertPolicySetting(any(), any(), any());
    }

    @Test
    @DisplayName("정책 설정 API는 로그인 사용자가 없으면 401을 반환한다")
    void policySettingApis_returnUnauthorized_whenNoLoginUser() throws Exception {
        PolicySettingController controller = new PolicySettingController(policySettingService);
        MockMvc unauthorizedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(null))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        unauthorizedMockMvc.perform(
                        get("/policies/settings")
                                .param("marketCode", "COUPANG")
                )
                .andExpect(status().isUnauthorized());

        unauthorizedMockMvc.perform(
                        put("/policies/settings")
                                .param("marketCode", "COUPANG")
                                .contentType("application/json")
                                .content(validPolicySettingRequest()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(policySettingService);
    }

    private PolicySettingResponse policySettingResponse() {
        return new PolicySettingResponse(
                1L,
                1L,
                MarketCode.COUPANG,
                BigDecimal.valueOf(35),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(1350),
                PriceRoundingUnit.HUNDRED_WON,
                false,
                false,
                true,
                true,
                false,
                true,
                ShippingFeeType.PAID_SHIPPING,
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(6000)
        );
    }

    private String validPolicySettingRequest() {
        return """
                {
                  "targetMarginRate": 35,
                  "minMarginAmount": 5000,
                  "marketFeeRate": 10,
                  "cardFeeRate": 3,
                  "exchangeRate": 1350,
                  "roundingUnit": "HUNDRED_WON",
                  "amazonAutoPricingEnabled": false,
                  "competitorAutoPricingEnabled": false,
                  "minMarginProtectEnabled": true,
                  "priceAutoUpdateEnabled": true,
                  "stopLossEnabled": false,
                  "autoPublishEnabled": true,
                  "shippingFeeType": "PAID_SHIPPING",
                  "baseShippingFee": 3000,
                  "remoteAreaExtraShippingFee": 5000,
                  "returnShippingFee": 6000
                }
                """;
    }

    private User createUser(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "username", "tester");
        ReflectionTestUtils.setField(user, "nickname", "테스터");
        return user;
    }

    private static class LoginUserTestArgumentResolver implements HandlerMethodArgumentResolver {

        private final User user;

        private LoginUserTestArgumentResolver(User user) {
            this.user = user;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(LoginUser.class)
                    && User.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            // 실제 ArgumentResolver 대신 테스트용 로그인 사용자를 그대로 주입한다.
            return user;
        }
    }
}
