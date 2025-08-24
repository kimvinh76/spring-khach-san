package com.example.demo.payment;

import java.util.Map;

import com.example.demo.model.Payment;

/**
 * PaymentProvider abstraction (adapter) for external payment providers.
 * Implementations should create a checkout/redirect URL and return provider-specific data
 * such as a provider reference (providerRef) which must be unique and used for idempotency.
 */
public interface PaymentProvider {
    /**
     * Create a checkout/redirect for a Payment.
     * @param payment the Payment record created in our system (status=PENDING)
     * @return a map containing at minimum redirectUrl and providerRef (provider's payment id)
     */
    Map<String, String> createCheckout(Payment payment);
}
