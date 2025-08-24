package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
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
import com.example.demo.service.BookingService;
import com.example.demo.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final ApplicationContext ctx;

    public PaymentApiController(PaymentService paymentService, BookingService bookingService, ApplicationContext ctx) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.ctx = ctx;
    }

    public static class InitiateRequest {
    private Long bookingId;
    private Double amount;
    private String method;
    private String provider;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    }

    private static final String KEY_PROVIDER_REF = "providerRef";
    private static final String KEY_REDIRECT = "redirectUrl";

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiate(@RequestBody InitiateRequest req) {
        if (req == null || req.bookingId == null || req.amount == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing fields"));
        }
        // Validate booking exists
        if (bookingService.findById(req.bookingId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "booking_not_found"));
        }
        String providerName = req.provider == null ? "mock" : req.provider;
        Payment p;
        try {
            p = paymentService.createPayment(req.bookingId, req.amount, req.method == null ? "external" : req.method, providerName);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "create_payment_failed", "message", ex.getMessage()));
        }

        // Lookup provider bean by name; providers are registered as Spring components with the bean name
        PaymentProvider provider;
        try {
            provider = (PaymentProvider) ctx.getBean(providerName);
        } catch (BeansException ex) {
            // If provider not found, fall back to mock behavior
            provider = (PaymentProvider) ctx.getBean("mock");
        }

        // Call provider to create checkout (redirect URL, providerRef)
        Map<String, String> provResp;
        try {
            provResp = provider.createCheckout(p);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "provider_checkout_failed", "message", ex.getMessage()));
        }
        // Save providerRef into payment for idempotency (if provider returned one)
        if (provResp != null) {
            String pref = provResp.get(KEY_PROVIDER_REF);
            if (pref != null) {
                p.setProviderRef(pref);
                paymentService.save(p);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("paymentId", p.getId());

        String redirect;
        if (provResp != null && provResp.get(KEY_REDIRECT) != null) {
            redirect = provResp.get(KEY_REDIRECT);
        } else {
            String ref = p.getProviderRef() == null ? String.valueOf(p.getId()) : p.getProviderRef();
            redirect = "/mock-provider/checkout?paymentId=" + p.getId() + "&" + KEY_PROVIDER_REF + "=" + ref;
        }

        resp.put(KEY_REDIRECT, redirect);
        resp.put(KEY_PROVIDER_REF, provResp == null ? null : provResp.get(KEY_PROVIDER_REF));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<com.example.demo.model.Payment> status(@RequestParam Long paymentId) {
        return paymentService.findById(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Dev-only helper to quickly create a sample booking so the initiate API can be tested
     * without going through the full booking flow. Remove or secure in production.
     */
    @PostMapping("/dev/create-sample-booking")
    public ResponseEntity<Map<String, Object>> createSampleBooking() {
        var booking = bookingService.createSampleBookingForTest();
        return ResponseEntity.ok(Map.of("bookingId", booking.getId()));
    }
}
