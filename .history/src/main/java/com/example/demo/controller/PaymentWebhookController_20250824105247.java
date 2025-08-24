package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Booking;
import com.example.demo.model.Invoice;
import com.example.demo.model.ServiceOrder;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.ServiceOrderService;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final BookingService bookingService;
    private final ServiceOrderService serviceOrderService;
    private final InvoiceRepository invoiceRepository;
    private final com.example.demo.service.PaymentService paymentService;

    @Value("${app.payment.webhook-secret:secret}")
    private String webhookSecret;

    private final com.example.demo.config.PaymentProperties paymentProperties;

    public PaymentWebhookController(BookingService bookingService,
                                    ServiceOrderService serviceOrderService,
                                    InvoiceRepository invoiceRepository,
                                    com.example.demo.service.PaymentService paymentService,
                                    com.example.demo.config.PaymentProperties paymentProperties) {
        this.bookingService = bookingService;
        this.serviceOrderService = serviceOrderService;
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
        this.paymentProperties = paymentProperties;
    }

    public static class PaymentPayload {
        private Long bookingId;
        private Double amount;
        private String paymentReference;
        private String provider;

        public Long getBookingId() { return bookingId; }
        public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getPaymentReference() { return paymentReference; }
        public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestHeader(value = "X-Payment-Secret", required = false) String secret,
                                          @RequestHeader(value = "X-Signature", required = false) String signature,
                                          @RequestBody String rawBody) {
        try {
            // Validate secret header first (backwards compatibility)
            if (secret == null || !secret.equals(webhookSecret)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid secret");
            }

            // Provider-specific verification
            if (signature != null && !signature.isEmpty()) {
                // If provider specified in payload, use provider-specific secret
                String provider = null;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper _m = new com.fasterxml.jackson.databind.ObjectMapper();
                    provider = _m.readTree(rawBody).path("provider").asText(null);
                } catch (Exception ex) {
                    // ignore
                }
                String secretToUse = webhookSecret;
                if (provider != null) {
                    if (provider.equalsIgnoreCase("stripe") && paymentProperties != null) {
                        secretToUse = paymentProperties.getStripe().getWebhookSecret();
                    } else if (provider.equalsIgnoreCase("momo") && paymentProperties != null) {
                        secretToUse = paymentProperties.getMomo().getSecretKey();
                    }
                }
                if (!verifyHmacSignatureWithSecret(rawBody, signature, secretToUse)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
                }
            }

            // Parse JSON body after signature validated
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            PaymentPayload payload = mapper.readValue(rawBody, PaymentPayload.class);

            if (payload == null || payload.bookingId == null || payload.amount == null) {
                return ResponseEntity.badRequest().body("missing bookingId or amount");
            }

            Booking booking = bookingService.findById(payload.bookingId).orElse(null);
            if (booking == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("booking not found");

            // Idempotency: if we have a providerRef (paymentReference) check if already handled
            if (payload.paymentReference != null) {
                java.util.Optional<com.example.demo.model.Payment> existing = paymentService.findByProviderRef(payload.paymentReference);
                if (existing.isPresent()) {
                    return ResponseEntity.ok("already processed");
                }
            }

            // Create payment record
            com.example.demo.model.Payment payment = paymentService.createPayment(payload.bookingId, payload.amount, "external", payload.provider == null ? "provider" : payload.provider);
            payment.setProviderRef(payload.paymentReference == null ? String.valueOf(payment.getId()) : payload.paymentReference);
            payment.setStatus("PAID");
            paymentService.save(payment);

            // Attach invoice and mark payable service orders as invoiced
            List<ServiceOrder> payable = serviceOrderService.findUnpaidConfirmedByBookingId(payload.bookingId);

            Invoice invoice = new Invoice();
            invoice.setCustomerName(booking.getCustomerName());
            invoice.setCreatedTime(java.time.LocalDateTime.now());
            invoice.setTotalAmount(payload.amount);
            invoice.setNote("Auto-created from webhook: " + (payload.paymentReference == null ? "" : payload.paymentReference));
            invoice = invoiceRepository.save(invoice);

            for (ServiceOrder s : payable) {
                s.setInvoice(invoice);
                serviceOrderService.save(s);
            }

            // Update booking status to include paid marker
            String status = booking.getStatus() == null ? "" : booking.getStatus();
            if (!status.contains("Đã thanh toán")) {
                booking.setStatus((status.isEmpty() ? "" : status + " - ") + "Đã thanh toán");
            }
            bookingService.save(booking);

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            LOGGER.error("webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error: " + e.getMessage());
        }
    }

    private boolean verifyHmacSignatureWithSecret(String rawBody, String signature, String secret) {
        if (secret == null) return false;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception ex) {
            LOGGER.warn("signature verification error", ex);
            return false;
        }
    }
}
