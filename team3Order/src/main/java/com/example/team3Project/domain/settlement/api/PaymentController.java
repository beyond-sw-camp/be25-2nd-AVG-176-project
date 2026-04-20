package com.example.team3Project.domain.settlement.api;

import com.example.team3Project.domain.settlement.application.PaymentService;
import com.example.team3Project.domain.settlement.dao.Payment;
import com.example.team3Project.domain.settlement.dto.PaymentRequest;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<Payment> pay(
            @LoginUser User user,
            @RequestBody PaymentRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(paymentService.processPayment(
                user.getId(),
                request.getOrderId(),
                request.getCardId()
        ));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Payment> refundPayment(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(paymentService.refundPayment(user.getId(), id));
    }
}
