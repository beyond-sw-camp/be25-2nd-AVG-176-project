package com.example.team3Project.domain.product.coupang.api;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.product.coupang.application.DummyCoupangProductService;
import com.example.team3Project.domain.product.coupang.dto.DummyCoupangProductResponse;
import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProduct;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DummyCoupangProductControllerTest {

    private MockMvc mockMvc;
    private DummyCoupangProductService dummyCoupangProductService;

    @BeforeEach
    void setUp() {
        dummyCoupangProductService = mock(DummyCoupangProductService.class);
        DummyCoupangProductController controller = new DummyCoupangProductController(dummyCoupangProductService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(createUser(1L, "tester", "테스터")))
                .build();
    }

    @Test
    @DisplayName("등록 후보 1건을 쿠팡 더미 상품으로 발행한다")
    void publish_returnsPublishedProduct() throws Exception {
        DummyCoupangProduct product = createProduct(30L, "ASIN-001", "쿠팡 상품명");
        when(dummyCoupangProductService.publishManually(1L, 10L)).thenReturn(product);

        mockMvc.perform(post("/products/registrations/10/publish/coupang"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dummyCoupangProductId").value(30))
                .andExpect(jsonPath("$.sourceProductId").value("ASIN-001"))
                .andExpect(jsonPath("$.productName").value("쿠팡 상품명"));
    }

    @Test
    @DisplayName("등록 후보 여러 건을 쿠팡 더미 상품으로 발행한다")
    void publishAll_returnsPublishedProducts() throws Exception {
        when(dummyCoupangProductService.publishAllManually(1L, List.of(10L, 11L)))
                .thenReturn(List.of(
                        createProduct(30L, "ASIN-001", "쿠팡 상품명"),
                        createProduct(31L, "ASIN-002", "쿠팡 상품명")
                ));

        mockMvc.perform(
                        post("/products/registrations/publish/coupang")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "registrationIds": [10, 11]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dummyCoupangProductId").value(30))
                .andExpect(jsonPath("$[1].dummyCoupangProductId").value(31));
    }

    @Test
    @DisplayName("쿠팡 상품 목록 API는 프론트 테이블용 DTO를 반환한다")
    void getProducts_returnsResponseDto() throws Exception {
        when(dummyCoupangProductService.getProducts(1L))
                .thenReturn(List.of(createResponse(30L, 10L, "ASIN-001", "coupang product")));

        mockMvc.perform(get("/products/coupang"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dummyCoupangProductId").value(30))
                .andExpect(jsonPath("$[0].registrationId").value(10))
                .andExpect(jsonPath("$[0].sourcingProductId").value("ASIN-001"))
                .andExpect(jsonPath("$[0].sourceMarket").value("AMAZON"))
                .andExpect(jsonPath("$[0].salePrice").value(33000))
                .andExpect(jsonPath("$[0].marginRate").value(18.18))
                .andExpect(jsonPath("$[0].status").value("SELLING"));
    }

    @Test
    @DisplayName("쿠팡 상품 검색 API는 검색 조건을 서비스로 전달하고 DTO를 반환한다")
    void searchProducts_passesSearchConditions() throws Exception {
        when(dummyCoupangProductService.searchRegisteredProducts(
                1L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 15),
                "amazon",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(30),
                "ASIN"
        )).thenReturn(List.of(createResponse(30L, 10L, "ASIN-001", "coupang product")));

        mockMvc.perform(
                        get("/products/coupang/search")
                                .param("from", "2026-04-01")
                                .param("to", "2026-04-15")
                                .param("source", "amazon")
                                .param("minMarginRate", "10")
                                .param("maxMarginRate", "30")
                                .param("keyword", "ASIN")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dummyCoupangProductId").value(30))
                .andExpect(jsonPath("$[0].registrationId").value(10))
                .andExpect(jsonPath("$[0].sourcingProductId").value("ASIN-001"))
                .andExpect(jsonPath("$[0].salePrice").value(33000))
                .andExpect(jsonPath("$[0].status").value("SELLING"));
    }

    @Test
    @DisplayName("쿠팡 더미 상품 상세 조회 API는 현재 로그인 사용자의 상품만 반환한다")
    void getProduct_returnsOwnedProduct() throws Exception {
        when(dummyCoupangProductService.getProduct(1L, 30L))
                .thenReturn(createProduct(30L, "ASIN-001", "쿠팡 상품명"));

        mockMvc.perform(get("/products/coupang/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dummyCoupangProductId").value(30))
                .andExpect(jsonPath("$.productName").value("쿠팡 상품명"));
    }

    @Test
    @DisplayName("로그인 사용자가 없으면 쿠팡 더미 상품 API는 401을 반환한다")
    void coupangApis_returnUnauthorized_whenNoLoginUser() throws Exception {
        DummyCoupangProductController controller = new DummyCoupangProductController(dummyCoupangProductService);
        MockMvc unauthorizedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(null))
                .build();

        unauthorizedMockMvc.perform(get("/products/coupang"))
                .andExpect(status().isUnauthorized());
        unauthorizedMockMvc.perform(get("/products/coupang/search"))
                .andExpect(status().isUnauthorized());
        unauthorizedMockMvc.perform(get("/products/coupang/30"))
                .andExpect(status().isUnauthorized());
        unauthorizedMockMvc.perform(post("/products/registrations/10/publish/coupang"))
                .andExpect(status().isUnauthorized());
        unauthorizedMockMvc.perform(
                        post("/products/registrations/publish/coupang")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "registrationIds": [10, 11]
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(dummyCoupangProductService);
    }

    private DummyCoupangProduct createProduct(Long productId, String sourceProductId, String productName) {
        DummyProductRegistration registration = DummyProductRegistration.create(
                1L,
                MarketCode.COUPANG,
                sourceProductId,
                "https://amazon.com/dp/" + sourceProductId,
                "https://image.example/" + sourceProductId + ".jpg",
                productName,
                "브랜드",
                BigDecimal.TEN,
                "USD",
                BigDecimal.valueOf(1350),
                BigDecimal.valueOf(13500),
                BigDecimal.valueOf(22000),
                BigDecimal.valueOf(3000),
                RegistrationStatus.REGISTERED,
                null
        );

        DummyCoupangProduct product = DummyCoupangProduct.create(registration);
        ReflectionTestUtils.setField(product, "dummyCoupangProductId", productId);
        return product;
    }

    private DummyCoupangProductResponse createResponse(
            Long productId,
            Long registrationId,
            String sourcingProductId,
            String productName
    ) {
        return new DummyCoupangProductResponse(
                productId,
                registrationId,
                sourcingProductId,
                productName,
                "AMAZON",
                BigDecimal.valueOf(33000),
                BigDecimal.valueOf(18.18),
                LocalDateTime.of(2026, 4, 15, 10, 0),
                "SELLING"
        );
    }

    private User createUser(Long id, String username, String nickname) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "username", username);
        ReflectionTestUtils.setField(user, "nickname", nickname);
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
            return user;
        }
    }
}
