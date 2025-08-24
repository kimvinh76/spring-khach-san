package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.model.Payment;

/**
 * Minimal MOMO provider stub. Replace with real HTTP integration and credentials.
 */
@Component("momo")
public class MomoPaymentProvider implements PaymentProvider {
    @Override
    public Map<String, String> createCheckout(Payment payment) {
        Map<String, String> resp = new HashMap<>();
        String providerRef = "MOMO-" + System.currentTimeMillis();
        String redirect = "/mock-provider/momo-checkout?paymentId=" + payment.getId() + "&providerRef=" + providerRef;
        resp.put("redirectUrl", redirect);
        resp.put("providerRef", providerRef);
        return resp;
    }
}
