package com.example.team3Project.domain.order.api;

import com.example.team3Project.domain.order.application.OrderService;
import com.example.team3Project.domain.order.dao.Order;
import com.example.team3Project.domain.order.dao.OrderRepository;
import com.example.team3Project.domain.order.dto.MonthlyRevenueResponse;
import com.example.team3Project.domain.order.dto.OrderCreateResponse;
import com.example.team3Project.domain.order.dto.OrderManagementResponse;
import com.example.team3Project.domain.order.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("주문이 없습니다."));
    }

    @PatchMapping("/{id}/cancel")
    public Order cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @GetMapping("/management")
    public List<OrderManagementResponse> getOrderManagement() {
        return orderService.getOrderManagement();
    }

    @GetMapping("/failed")
    public List<Order> getFailedOrders() {
        return orderService.getFailedOrders();
    }

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(@PathVariable Long userId) {
        return orderService.getOrdersByUser(userId);
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<MonthlyRevenueResponse>> getMonthlyRevenue(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = parseUserIdHeader(userIdHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getMonthlyRevenue(userId));
    }

    private Long parseUserIdHeader(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        String firstValue = userIdHeader.split(",")[0].trim();
        if (firstValue.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(firstValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
