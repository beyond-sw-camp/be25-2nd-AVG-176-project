package com.example.team3Project.domain.settlement.application;

import com.example.team3Project.domain.order.dao.Order;
import com.example.team3Project.domain.order.dao.OrderRepository;
import com.example.team3Project.domain.settlement.dao.Card;
import com.example.team3Project.domain.settlement.dao.CardRepository;
import com.example.team3Project.domain.settlement.dao.Payment;
import com.example.team3Project.domain.settlement.dao.PaymentRepository;
import com.example.team3Project.domain.settlement.dto.DecryptedCardInfo;
import com.example.team3Project.domain.settlement.enums.PaymentStatus;
import com.example.team3Project.domain.shipment.application.ShipmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final OrderRepository orderRepository;
    private final CardService cardService;
    private final ShipmentService shipmentService;

    @Transactional
    public Payment processPayment(Long userId, Long orderId, Long cardId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (cardId == null) {
            return processPaymentByPriority(userId, order);
        }

        return processPaymentWithCard(userId, order, cardId);
    }

    private Payment processPaymentByPriority(Long userId, Order order) {
        List<Card> cards = cardRepository.findByUserIdAndActiveTrueOrderByIdAsc(userId);
        if (cards.isEmpty()) {
            return failPayment(order, createPayment(order), PaymentStatus.FAILED_CARD_NOT_FOUND);
        }

        Payment lastFailedPayment = null;
        for (Card card : cards) {
            Payment payment = processPaymentWithCard(userId, order, card.getId());
            if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                return payment;
            }
            lastFailedPayment = payment;
        }

        return lastFailedPayment;
    }

    private Payment processPaymentWithCard(Long userId, Order order, Long cardId) {
        Payment payment = createPayment(order);

        Card card = cardRepository.findByIdAndUserId(cardId, userId).orElse(null);
        if (card == null) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_NOT_FOUND);
        }

        payment.setCard(card);

        if (!card.isActive()) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_INACTIVE);
        }

        DecryptedCardInfo cardInfo = cardService.getDecryptedCard(userId, cardId);
        if (isCardExpired(cardInfo.getExpiry())) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_EXPIRED);
        }

        if (payment.getAmount() > card.getCardLimit()) {
            return failPayment(order, payment, PaymentStatus.FAILED_LIMIT_EXCEEDED);
        }

        if (payment.getAmount() > card.getBalance()) {
            return failPayment(order, payment, PaymentStatus.FAILED_INSUFFICIENT_BALANCE);
        }

        card.setBalance(card.getBalance() - payment.getAmount());
        card.setCardLimit(card.getCardLimit() - payment.getAmount());
        cardRepository.save(card);

        payment.setStatus(PaymentStatus.SUCCESS.name());
        order.setStatus("PAID");
        order.setAutoOrderStatus("Ordered");
        shipmentService.createDefaultShipment(order.getId());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Card card = payment.getCard();
        if (card == null || card.getUserId() == null || !card.getUserId().equals(userId)) {
            throw new RuntimeException("Payment not found");
        }

        if (!PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            throw new RuntimeException("Payment cannot be refunded.");
        }

        Order order = payment.getOrder();

        card.setBalance(card.getBalance() + payment.getAmount());
        card.setCardLimit(card.getCardLimit() + payment.getAmount());
        cardRepository.save(card);

        payment.setStatus("REFUNDED");
        order.setStatus("FAILED");
        order.setAutoOrderStatus("FAILED");

        return paymentRepository.save(payment);
    }

    private Payment createPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(getPaymentAmount(order));
        return payment;
    }

    private int getPaymentAmount(Order order) {
        return Math.max(order.getTotalAmount() - order.getMargin(), 0);
    }

    private Payment failPayment(Order order, Payment payment, PaymentStatus status) {
        payment.setStatus(status.name());
        order.setStatus("FAILED");
        order.setAutoOrderStatus("FAILED");
        return paymentRepository.save(payment);
    }

    private boolean isCardExpired(String expiry) {
        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = 2000 + Integer.parseInt(parts[1]);

        YearMonth cardDate = YearMonth.of(year, month);
        YearMonth now = YearMonth.now();

        return cardDate.isBefore(now);
    }
}
