package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Payment;
import com.example.demo.payment.PaymentProvider;
import com.example.demo.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {
    private final PaymentService paymentService;
    private final ApplicationContext ctx;

    public PaymentApiController(PaymentService paymentService, ApplicationContext ctx) {
        this.paymentService = paymentService;
        this.ctx = ctx;
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
        String providerName = req.provider == null ? "mock" : req.provider;
        Payment p = paymentService.createPayment(req.bookingId, req.amount, req.method == null ? "external" : req.method, providerName);

        // Lookup provider bean by name; providers are registered as Spring components with the bean name
        PaymentProvider provider;
        try {
            provider = (PaymentProvider) ctx.getBean(providerName);
        } catch (Exception ex) {
            // If provider not found, fall back to mock behavior
            provider = (PaymentProvider) ctx.getBean("mock");
        }

        // Call provider to create checkout (redirect URL, providerRef)
        Map<String, String> provResp = provider.createCheckout(p);
        // Save providerRef into payment for idempotency
        if (provResp != null && provResp.get("providerRef") != null) {
            p.setProviderRef(provResp.get("providerRef"));
            paymentService.save(p);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("paymentId", p.getId());
        resp.put("redirectUrl", provResp.getOrDefault("redirectUrl", "/mock-provider/checkout?paymentId=" + p.getId()));
        resp.put("providerRef", provResp.get("providerRef"));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam Long paymentId) {
        return paymentService.findById(paymentId)
                .map(p -> ResponseEntity.ok(p))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
