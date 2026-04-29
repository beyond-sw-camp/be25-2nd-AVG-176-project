package com.example.team3Project.domain.order.api;

import com.example.team3Project.domain.order.application.OrderService;
import com.example.team3Project.domain.order.dao.Order;
import com.example.team3Project.domain.order.dto.FailedOrderResponse;
import com.example.team3Project.domain.order.dto.MonthlyRevenueResponse;
import com.example.team3Project.domain.order.dto.OrderCreateResponse;
import com.example.team3Project.domain.order.dto.OrderManagementResponse;
import com.example.team3Project.domain.order.dto.OrderRequest;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/orders", "/api/orders"})
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            @LoginUser User user,
            @RequestBody OrderRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.createOrder(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getOrdersByUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getOrder(user.getId(), id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.cancelOrder(user.getId(), id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<OrderCreateResponse> retryOrder(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.retryOrder(user.getId(), id));
    }

    @GetMapping("/management")
    public ResponseEntity<List<OrderManagementResponse>> getOrderManagement(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getOrderManagement(user.getId()));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<FailedOrderResponse>> getFailedOrders(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getFailedOrders(user.getId()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(
            @LoginUser User user,
            @PathVariable Long userId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getOrdersByUser(user.getId()));
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<MonthlyRevenueResponse>> getMonthlyRevenue(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(orderService.getMonthlyRevenue(user.getId()));
    }
}
