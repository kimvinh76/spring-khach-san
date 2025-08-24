package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.model.Payment;

/**
 * Mock provider used for local testing. In production, implement providers for MOMO/Stripe/Napas.
 */
@Component("mock")
public class MockPaymentProvider implements PaymentProvider {
    @Override
    public Map<String, String> createCheckout(Payment payment) {
        Map<String, String> resp = new HashMap<>();
        // In a real provider, you would call their API and get a provider-specific id & redirect URL.
        String providerRef = "MOCK-" + payment.getId();
        String redirect = "/mock-provider/checkout?paymentId=" + payment.getId() + "&providerRef=" + providerRef;
        resp.put("redirectUrl", redirect);
        resp.put("providerRef", providerRef);
        return resp;
    }
}
