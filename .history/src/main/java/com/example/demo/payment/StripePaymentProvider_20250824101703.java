package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.model.Payment;

/**
 * Minimal Stripe provider stub. Replace with real Stripe Checkout integration.
 */
@Component("stripe")
public class StripePaymentProvider implements PaymentProvider {
    @Override
    public Map<String, String> createCheckout(Payment payment) {
        Map<String, String> resp = new HashMap<>();
        String providerRef = "STRIPE-" + System.currentTimeMillis();
        String redirect = "/mock-provider/stripe-checkout?paymentId=" + payment.getId() + "&providerRef=" + providerRef;
        resp.put("redirectUrl", redirect);
        resp.put("providerRef", providerRef);
        return resp;
    }
}
