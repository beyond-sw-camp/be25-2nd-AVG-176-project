package com.example.team3Project.domain.order.application;

import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProduct;
import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProductOption;
import com.example.team3Project.domain.dummyMarket.dao.DummyMarketCoupangProductRepository;
import com.example.team3Project.domain.order.dao.Order;
import com.example.team3Project.domain.order.dao.OrderRepository;
import com.example.team3Project.domain.order.dto.FailedOrderResponse;
import com.example.team3Project.domain.order.dto.MonthlyRevenueResponse;
import com.example.team3Project.domain.order.dto.OrderCreateResponse;
import com.example.team3Project.domain.order.dto.OrderManagementResponse;
import com.example.team3Project.domain.order.dto.OrderRequest;
import com.example.team3Project.domain.settlement.application.PaymentService;
import com.example.team3Project.domain.settlement.dao.Card;
import com.example.team3Project.domain.settlement.dao.Payment;
import com.example.team3Project.domain.settlement.dao.PaymentRepository;
import com.example.team3Project.domain.settlement.enums.PaymentStatus;
import com.example.team3Project.domain.settlement.util.CardNumberUtil;
import com.example.team3Project.domain.settlement.util.PaymentFailureReasonUtil;
import com.example.team3Project.domain.shipment.enums.Shipment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final DummyMarketCoupangProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderRequest request) {
        if (request.getDummyCoupangProductId() == null) {
            throw new RuntimeException("주문 상품이 없습니다.");
        }
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("주문 수량은 1 이상이어야 합니다.");
        }
        DummyCoupangProduct product = productRepository.findById(request.getDummyCoupangProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + request.getDummyCoupangProductId()));

        if (product.getUserId() != null && !product.getUserId().equals(userId)) {
            throw new RuntimeException("상품을 찾을 수 없습니다.");
        }

        Optional<DummyCoupangProductOption> selectedOption = findSelectedOption(product, request);
        BigDecimal salePrice = selectedOption
                .map(DummyCoupangProductOption::getSalePrice)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .orElse(product.getSalePrice());
        if (salePrice == null) {
            throw new RuntimeException("상품 가격 정보가 없습니다.");
        }
        salePrice = salePrice.setScale(0, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setUserId(userId);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setCustomsNumber(request.getCustomsNumber());
        order.setDummyCoupangProduct(product);
        order.setProductName(product.getProductName());

        int quantity = request.getQuantity();
        order.setQuantity(quantity);
        order.setTotalAmount(salePrice.multiply(BigDecimal.valueOf(quantity)).intValue());

        int orderMargin = product.getMarginKrw() != null
                ? product.getMarginKrw().multiply(BigDecimal.valueOf(quantity)).intValue()
                : 0;
        order.setMargin(orderMargin);

        order.setStatus("FAILED");
        order.setAutoOrderStatus("FAILED");

        Order savedOrder = orderRepository.save(order);
        Payment payment = paymentService.processPayment(userId, savedOrder.getId(), null);

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
    public OrderCreateResponse retryOrder(Long userId, Long orderId) {
        Order order = getOrder(userId, orderId);
        if ("PAID".equals(order.getStatus())) {
            throw new RuntimeException("이미 결제 완료된 주문입니다.");
        }

        Payment payment = paymentService.processPayment(userId, order.getId(), null);
        boolean success = PaymentStatus.SUCCESS.name().equals(payment.getStatus());
        if (success && order.getDummyCoupangProduct() != null) {
            order.getDummyCoupangProduct().incrementOrderCount(order.getQuantity());
        }

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus())
                .autoOrderStatus(order.getAutoOrderStatus())
                .paymentStatus(payment.getStatus())
                .success(success)
                .message(success
                        ? "주문과 결제가 완료되었습니다."
                        : "AutoSource 시스템을 이용한 상품 주문에 실패했습니다.")
                .build();
    }

    @Transactional
    public Order cancelOrder(Long userId, Long orderId) {
        Order order = getOrder(userId, orderId);

        if ("PAID".equals(order.getStatus())) {
            throw new RuntimeException("결제된 주문은 취소할 수 없습니다.");
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("주문이 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<OrderManagementResponse> getOrderManagement(Long userId) {
        return orderRepository.findAllWithShipmentByProductUserId(userId).stream()
                .map(this::toManagementResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FailedOrderResponse> getFailedOrders(Long userId) {
        return orderRepository.findByAutoOrderStatusAndProductUserId("FAILED", userId).stream()
                .map(this::toFailedOrderResponse)
                .toList();
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

    private OrderManagementResponse toManagementResponse(Order order) {
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
    }

    private FailedOrderResponse toFailedOrderResponse(Order order) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByIdDesc(order.getId())
                .orElse(null);
        String paymentStatus = payment != null ? payment.getStatus() : null;
        Card card = payment != null ? payment.getCard() : null;

        return FailedOrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .autoOrderStatus(order.getAutoOrderStatus())
                .paymentFailureCode(paymentStatus)
                .paymentFailureReason(PaymentFailureReasonUtil.toKoreanMessage(paymentStatus))
                .cardId(card != null ? card.getId() : null)
                .cardType(card != null ? card.getCardType() : null)
                .cardLast4(card != null ? CardNumberUtil.last4(card.getCardNumber()) : null)
                .cardActive(card != null ? card.isActive() : null)
                .cardLimit(card != null ? card.getCardLimit() : null)
                .cardBalance(card != null ? card.getBalance() : null)
                .configuredLimit(card != null ? card.getCardLimit() : null)
                .build();
    }

    private String getCardLast4(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return null;
        }

        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return digits;
        }

        return digits.substring(digits.length() - 4);
    }

    private String toPaymentFailureReason(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return null;
        }

        return switch (paymentStatus) {
            case "FAILED_CARD_NOT_FOUND" -> "카드를 찾을 수 없습니다.";
            case "FAILED_CARD_OWNER_MISMATCH" -> "주문자와 카드 소유자가 일치하지 않습니다.";
            case "FAILED_CARD_INACTIVE" -> "비활성화된 카드입니다.";
            case "FAILED_CVC_MISMATCH" -> "CVC가 일치하지 않습니다.";
            case "FAILED_CARD_EXPIRED" -> "만료된 카드입니다.";
            case "FAILED_LIMIT_EXCEEDED" -> "카드 한도를 초과했습니다.";
            case "FAILED_INSUFFICIENT_BALANCE" -> "카드 잔액이 부족합니다.";
            default -> paymentStatus;
        };
    }

    private Optional<DummyCoupangProductOption> findSelectedOption(
            DummyCoupangProduct product,
            OrderRequest request
    ) {
        Long optionId = request.getDummyCoupangProductOptionId();
        if (optionId != null) {
            DummyCoupangProductOption option = product.getOptions().stream()
                    .filter(opt -> optionId.equals(opt.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("선택한 옵션을 찾을 수 없습니다."));
            return Optional.of(option);
        }

        return product.getOptions().stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getSelected()))
                .findFirst();
    }
}
