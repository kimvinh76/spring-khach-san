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
    private static final String KEY_ERROR = "error";
    private static final String ERR_MISSING_FIELDS = "missing_fields";
    private static final String ERR_BOOKING_NOT_FOUND = "booking_not_found";
    private static final String ERR_CREATE_PAYMENT_FAILED = "create_payment_failed";
    private static final String ERR_PROVIDER_CHECKOUT_FAILED = "provider_checkout_failed";

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiate(@RequestBody InitiateRequest req) {
        var validationError = validateRequest(req);
        if (validationError != null) return validationError;

        var providerName = req.provider == null ? "mock" : req.provider;
        var method = req.method == null ? "external" : req.method;
        var paymentResult = safeCreatePayment(req.bookingId, req.amount, method, providerName);
        if (paymentResult.get(KEY_ERROR) != null) return ResponseEntity.internalServerError().body(paymentResult);
        Payment p = (Payment) paymentResult.get("payment");

        var provider = resolveProvider(providerName);
        var checkoutResult = safeProviderCheckout(provider, p);
        if (checkoutResult.get(KEY_ERROR) != null) return ResponseEntity.internalServerError().body(checkoutResult);
        @SuppressWarnings("unchecked")
        Map<String, String> provResp = (Map<String, String>) checkoutResult.get("resp");

        updatePaymentWithProviderRef(p, provResp);
        return ResponseEntity.ok(buildSuccessResponse(p, provResp));
    }

    private ResponseEntity<Map<String, Object>> validateRequest(InitiateRequest req) {
        if (req == null || req.bookingId == null || req.amount == null) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, ERR_MISSING_FIELDS));
        }
        if (bookingService.findById(req.bookingId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, ERR_BOOKING_NOT_FOUND));
        }
        return null;
    }

    private Map<String, Object> safeCreatePayment(Long bookingId, Double amount, String method, String provider) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("payment", paymentService.createPayment(bookingId, amount, method, provider));
        } catch (Exception ex) {
            result.put(KEY_ERROR, ERR_CREATE_PAYMENT_FAILED);
            result.put("message", ex.getMessage());
        }
        return result;
    }

    private PaymentProvider resolveProvider(String providerName) {
        try {
            return (PaymentProvider) ctx.getBean(providerName);
        } catch (BeansException ex) {
            return (PaymentProvider) ctx.getBean("mock");
        }
    }

    private Map<String, Object> safeProviderCheckout(PaymentProvider provider, Payment p) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("resp", provider.createCheckout(p));
        } catch (Exception ex) {
            result.put(KEY_ERROR, ERR_PROVIDER_CHECKOUT_FAILED);
            result.put("message", ex.getMessage());
        }
        return result;
    }

    private void updatePaymentWithProviderRef(Payment p, Map<String, String> provResp) {
        if (provResp == null) return;
        String pref = provResp.get(KEY_PROVIDER_REF);
        if (pref != null) {
            p.setProviderRef(pref);
            paymentService.save(p);
        }
    }

    private Map<String, Object> buildSuccessResponse(Payment p, Map<String, String> provResp) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("paymentId", p.getId());
        String redirect = (provResp != null && provResp.get(KEY_REDIRECT) != null)
                ? provResp.get(KEY_REDIRECT)
                : "/mock-provider/checkout?paymentId=" + p.getId() + "&" + KEY_PROVIDER_REF + "=" +
                        (p.getProviderRef() == null ? p.getId() : p.getProviderRef());
        resp.put(KEY_REDIRECT, redirect);
        resp.put(KEY_PROVIDER_REF, provResp == null ? null : provResp.get(KEY_PROVIDER_REF));
        return resp;
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
