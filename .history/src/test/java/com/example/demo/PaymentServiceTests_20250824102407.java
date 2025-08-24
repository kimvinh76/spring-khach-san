package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.model.Payment;
import com.example.demo.service.PaymentService;

@SpringBootTest
class PaymentServiceTests {

    @Autowired
    PaymentService paymentService;

    @Test
    void createPaymentAssignsFields() {
        Payment p = paymentService.createPayment(123L, 450000.0, "external", "mock");
        assertThat(p.getId()).isNotNull();
        assertThat(p.getStatus()).isEqualTo("PENDING");
        assertThat(p.getProvider()).isEqualTo("mock");
    }
}
