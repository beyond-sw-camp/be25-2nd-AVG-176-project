package com.example.team3Project.global.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginCheckInterceptorTest {

    private final LoginCheckInterceptor interceptor =
            new LoginCheckInterceptor();

    @Test
    @DisplayName("인증 헤더가 없으면 401을 반환한다")
    void preHandle_returnsUnauthorized_whenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products/registrations");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("프리플라이트 OPTIONS 요청은 통과한다")
    void preHandle_allowsOptionsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/products/coupang");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("인증 헤더가 있으면 요청을 통과한다")
    void preHandle_allowsRequest_whenHeaderExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products/coupang");
        request.addHeader("X-User-Id", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }
}
