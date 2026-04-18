package com.example.team3Project.domain.order.application;

import com.example.team3Project.domain.order.dao.Order;
import com.example.team3Project.domain.order.dao.OrderRepository;
import com.example.team3Project.domain.order.dto.MonthlyRevenueResponse;
import com.example.team3Project.domain.order.dto.OrderCreateResponse;
import com.example.team3Project.domain.order.dto.OrderManagementResponse;
import com.example.team3Project.domain.order.dto.OrderRequest;
import com.example.team3Project.domain.product.dao.DummyCoupangProduct;
import com.example.team3Project.domain.product.dao.DummyCoupangProductRepository;
import com.example.team3Project.domain.settlement.application.PaymentService;
import com.example.team3Project.domain.settlement.dao.Payment;
import com.example.team3Project.domain.settlement.enums.PaymentStatus;
import com.example.team3Project.domain.shipment.enums.Shipment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final DummyCoupangProductRepository productRepository;

    @Transactional
    public OrderCreateResponse createOrder(OrderRequest request) {
        if (request.getDummyCoupangProductId() == null) {
            throw new RuntimeException("주문 상품이 없습니다.");
        }
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("주문 수량은 1 이상이어야 합니다.");
        }
        if (request.getCardId() == null) {
            throw new RuntimeException("결제 카드를 선택해 주세요.");
        }

        DummyCoupangProduct product = productRepository.findById(request.getDummyCoupangProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + request.getDummyCoupangProductId()));

        Order order = new Order();
        order.setUserId(request.getUserId() != null ? request.getUserId() : product.getUserId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setCustomsNumber(request.getCustomsNumber());
        order.setDummyCoupangProduct(product);
        order.setProductName(product.getProductName());

        int quantity = request.getQuantity();
        order.setQuantity(quantity);
        order.setTotalAmount(product.getSalePrice().multiply(BigDecimal.valueOf(quantity)).intValue());

        int orderMargin = product.getMarginKrw() != null
                ? product.getMarginKrw().multiply(BigDecimal.valueOf(quantity)).intValue()
                : 0;
        order.setMargin(orderMargin);

        order.setStatus("FAILED");
        order.setAutoOrderStatus("FAILED");

        Order savedOrder = orderRepository.save(order);
        Payment payment = paymentService.processPayment(savedOrder.getId(), request.getCardId());

        boolean success = PaymentStatus.SUCCESS.name().equals(payment.getStatus());
        if (success) {
            product.incrementOrderCount(quantity);
        }

        return OrderCreateResponse.builder()
                .orderId(savedOrder.getId())
                .orderStatus(savedOrder.getStatus())
                .autoOrderStatus(savedOrder.getAutoOrderStatus())
                .paymentStatus(payment.getStatus())
                .success(success)
                .message(success
                        ? "주문과 결제가 완료되었습니다."
                        : "AutoSource 시스템을 이용한 상품 주문에 실패했습니다.")
                .build();
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문이 없습니다."));

        if ("PAID".equals(order.getStatus())) {
            throw new RuntimeException("결제된 주문은 취소할 수 없습니다.");
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderManagementResponse> getOrderManagement() {
        List<Order> orders = orderRepository.findAllWithShipment();

        return orders.stream().map(order -> {
            OrderManagementResponse res = new OrderManagementResponse();
            res.setOrderId(order.getId());
            res.setAutoOrderStatus(order.getAutoOrderStatus());
            res.setCustomerName(order.getCustomerName());
            res.setCustomerPhone(order.getCustomerPhone());
            res.setCustomerAddress(order.getCustomerAddress());
            res.setCustomsNumber(order.getCustomsNumber());
            res.setDummyCoupangProductId(order.getDummyCoupangProductId());
            res.setProductName(order.getProductName());
            res.setQuantity(order.getQuantity());
            res.setOverseasMall(order.getOverseasMall());
            res.setPaymentAmount(order.getTotalAmount());
            res.setMargin(order.getMargin());

            Shipment shipment = order.getShipment();
            if (shipment != null) {
                res.setShipmentId(shipment.getId());
                res.setShipmentStatus(shipment.getStatus().name());
                res.setTrackingNumber(shipment.getTrackingNumber());
                res.setCourier(shipment.getCourier());
            }

            return res;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Order> getFailedOrders() {
        return orderRepository.findByAutoOrderStatus("FAILED");
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueResponse> getMonthlyRevenue(Long userId) {
        return orderRepository.findMonthlyRevenueByUserId(userId).stream()
                .map(row -> {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    long orderCount = ((Number) row[2]).longValue();
                    long sales = ((Number) row[3]).longValue();
                    long margin = ((Number) row[4]).longValue();
                    double profitRate = sales > 0 ? (margin * 100.0 / sales) : 0.0;

                    return new MonthlyRevenueResponse(year, month, orderCount, sales, margin, profitRate);
                })
                .toList();
    }
}
