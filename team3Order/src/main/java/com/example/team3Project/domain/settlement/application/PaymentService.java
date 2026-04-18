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

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final OrderRepository orderRepository;
    private final CardService cardService;
    private final ShipmentService shipmentService;

    @Transactional
    public Payment processPayment(Long orderId, Long cardId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());

        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_NOT_FOUND);
        }

        payment.setCard(card);

        Long sellerUserId = order.getDummyCoupangProduct() != null
                ? order.getDummyCoupangProduct().getUserId()
                : null;
        if (sellerUserId != null && card.getUserId() != null && !sellerUserId.equals(card.getUserId())) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_OWNER_MISMATCH);
        }

        if (!card.isActive()) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_INACTIVE);
        }

        DecryptedCardInfo cardInfo = cardService.getDecryptedCard(cardId);
        if (isCardExpired(cardInfo.getExpiry())) {
            return failPayment(order, payment, PaymentStatus.FAILED_CARD_EXPIRED);
        }

        if (order.getTotalAmount() > card.getCardLimit()) {
            return failPayment(order, payment, PaymentStatus.FAILED_LIMIT_EXCEEDED);
        }

        if (order.getTotalAmount() > card.getBalance()) {
            return failPayment(order, payment, PaymentStatus.FAILED_INSUFFICIENT_BALANCE);
        }

        card.setBalance(card.getBalance() - order.getTotalAmount());
        card.setCardLimit(card.getCardLimit() - order.getTotalAmount());
        cardRepository.save(card);

        payment.setStatus(PaymentStatus.SUCCESS.name());
        order.setStatus("PAID");
        order.setAutoOrderStatus("Ordered");
        shipmentService.createDefaultShipment(order.getId());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제를 찾을 수 없습니다."));

        if (!PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            throw new RuntimeException("환불할 수 없는 결제입니다.");
        }

        Card card = payment.getCard();
        Order order = payment.getOrder();

        card.setBalance(card.getBalance() + payment.getAmount());
        card.setCardLimit(card.getCardLimit() + payment.getAmount());
        cardRepository.save(card);

        payment.setStatus("REFUNDED");
        order.setStatus("FAILED");
        order.setAutoOrderStatus("FAILED");

        return paymentRepository.save(payment);
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
