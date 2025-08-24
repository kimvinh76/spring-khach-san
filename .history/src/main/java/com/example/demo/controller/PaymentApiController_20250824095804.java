package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Payment;
import com.example.demo.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {
    private final PaymentService paymentService;

    public PaymentApiController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public static class InitiateRequest {
        public Long bookingId;
        public Double amount;
        public String method;
        public String provider;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody InitiateRequest req) {
        if (req == null || req.bookingId == null || req.amount == null) {
            return ResponseEntity.badRequest().body("missing fields");
        }
        Payment p = paymentService.createPayment(req.bookingId, req.amount, req.method == null ? "external" : req.method, req.provider == null ? "mock" : req.provider);

        // For now return a mock redirect URL simulating the external provider checkout
        Map<String, Object> resp = new HashMap<>();
        resp.put("paymentId", p.getId());
        resp.put("redirectUrl", "/mock-provider/checkout?paymentId=" + p.getId());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam Long paymentId) {
        return paymentService.findByProviderRef(String.valueOf(paymentId))
                .map(p -> ResponseEntity.ok(p))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
