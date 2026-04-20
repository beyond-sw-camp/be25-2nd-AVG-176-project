package com.example.team3Project.domain.order.application;

import com.example.team3Project.domain.order.dao.OrderRepository;
import com.example.team3Project.domain.order.dto.MonthlyRevenueResponse;
import com.example.team3Project.domain.product.dao.DummyCoupangProductRepository;
import com.example.team3Project.domain.settlement.application.PaymentService;
import com.example.team3Project.domain.settlement.dao.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DummyCoupangProductRepository productRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void getMonthlyRevenueMapsRepositoryRowsToResponse() {
        OrderService orderService = new OrderService(
                orderRepository,
                paymentService,
                productRepository,
                paymentRepository
        );
        when(orderRepository.findMonthlyRevenueByUserId(7L))
                .thenReturn(List.<Object[]>of(new Object[]{2026, 4, 3L, 100_000L, 25_000L}));

        List<MonthlyRevenueResponse> result = orderService.getMonthlyRevenue(7L);

        assertThat(result).hasSize(1);
        MonthlyRevenueResponse response = result.get(0);
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(4);
        assertThat(response.getOrderCount()).isEqualTo(3L);
        assertThat(response.getSales()).isEqualTo(100_000L);
        assertThat(response.getMargin()).isEqualTo(25_000L);
        assertThat(response.getProfitRate()).isEqualTo(25.0);
    }

    @Test
    void getMonthlyRevenueUsesZeroProfitRateWhenSalesIsZero() {
        OrderService orderService = new OrderService(
                orderRepository,
                paymentService,
                productRepository,
                paymentRepository
        );
        when(orderRepository.findMonthlyRevenueByUserId(7L))
                .thenReturn(List.<Object[]>of(new Object[]{2026, 4, 1L, 0L, 10_000L}));

        List<MonthlyRevenueResponse> result = orderService.getMonthlyRevenue(7L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProfitRate()).isZero();
    }
}
