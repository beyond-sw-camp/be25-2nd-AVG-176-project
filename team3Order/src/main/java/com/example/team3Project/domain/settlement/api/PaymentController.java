package com.example.team3Project.domain.settlement.api;

import com.example.team3Project.domain.settlement.application.PaymentService;
import com.example.team3Project.domain.settlement.dao.Payment;
import com.example.team3Project.domain.settlement.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/settlements")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 요청 처리
    @PostMapping
    public Payment pay(@RequestBody PaymentRequest request) {
        return paymentService.processPayment(
                request.getOrderId(),
                request.getCardId()
        );
    }

    // 환불 요청 처리
    @PostMapping("/{id}/refund")
    public Payment refundPayment(@PathVariable Long id) {
        return paymentService.refundPayment(id);
    }
}