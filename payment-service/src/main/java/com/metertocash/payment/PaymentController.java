package com.metertocash.payment;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentRepository payments;

    public PaymentController(PaymentRepository payments) {
        this.payments = payments;
    }

    @GetMapping("/payments/{billId}")
    public List<PaymentEntity> payments(@PathVariable String billId) {
        return payments.findByBillIdOrderByIdAsc(billId);
    }
}
