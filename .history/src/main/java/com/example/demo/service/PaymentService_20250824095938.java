package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.model.Payment;
import com.example.demo.repository.PaymentRepository;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(Long bookingId, Double amount, String method, String provider) {
        Payment p = new Payment();
        p.setBookingId(bookingId);
        p.setAmount(amount);
        p.setMethod(method);
        p.setProvider(provider);
        p.setStatus("PENDING");
        p.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(p);
    }

    public Optional<Payment> findByProviderRef(String ref) {
        return paymentRepository.findByProviderRef(ref);
    }

    public Payment save(Payment p) {
        return paymentRepository.save(p);
    }
}
