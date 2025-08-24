package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;

/**
 * Stripe provider integration scaffold.
 * TODO: integrate with Stripe Checkout session creation.
 * Example properties:
 *   app.payment.stripe.apiKey=
 *   app.payment.stripe.webhookSecret=
 */
@org.springframework.stereotype.Component("stripe")
public class StripePaymentProvider implements PaymentProvider {
    // TODO: inject Stripe API key and call stripe.checkout.sessions.create
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
