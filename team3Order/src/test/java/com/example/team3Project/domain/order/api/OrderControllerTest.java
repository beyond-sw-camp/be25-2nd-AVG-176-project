package com.example.team3Project.domain.order.api;

import com.example.team3Project.domain.order.application.OrderService;
import com.example.team3Project.domain.order.dto.MonthlyRevenueResponse;
import com.example.team3Project.global.resolver.LoginUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new OrderController(orderService))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .build();
    }

    @Test
    void getMonthlyRevenueRequiresLoginUser() throws Exception {
        mockMvc.perform(get("/orders/revenue/monthly"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    void getMonthlyRevenueReturnsMonthlyRevenueResponses() throws Exception {
        when(orderService.getMonthlyRevenue(7L))
                .thenReturn(List.of(new MonthlyRevenueResponse(2026, 4, 3L, 100_000L, 25_000L, 25.0)));

        mockMvc.perform(get("/orders/revenue/monthly").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].month").value(4))
                .andExpect(jsonPath("$[0].orderCount").value(3))
                .andExpect(jsonPath("$[0].sales").value(100_000))
                .andExpect(jsonPath("$[0].margin").value(25_000))
                .andExpect(jsonPath("$[0].profitRate").value(25.0));

        verify(orderService).getMonthlyRevenue(7L);
    }
}
