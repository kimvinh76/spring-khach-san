package com.example.demo.controller;

import java.util.List;

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

    private final BookingService bookingService;
    private final ServiceOrderService serviceOrderService;
    private final InvoiceRepository invoiceRepository;
    private final com.example.demo.service.PaymentService paymentService;

    @Value("${app.payment.webhook-secret:secret}")
    private String webhookSecret;

    public PaymentWebhookController(BookingService bookingService,
                                    ServiceOrderService serviceOrderService,
                                    InvoiceRepository invoiceRepository,
                                    com.example.demo.service.PaymentService paymentService) {
        this.bookingService = bookingService;
        this.serviceOrderService = serviceOrderService;
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
    }

    public static class PaymentPayload {
        public Long bookingId;
        public Double amount;
        public String paymentReference;
        public String provider;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestHeader(value = "X-Payment-Secret", required = false) String secret,
                                          @RequestBody PaymentPayload payload) {
        try {
            if (secret == null || !secret.equals(webhookSecret)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid secret");
            }

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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error: " + e.getMessage());
        }
    }
}
