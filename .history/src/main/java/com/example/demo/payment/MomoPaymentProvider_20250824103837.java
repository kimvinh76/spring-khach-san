package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;

import com.example.demo.model.Payment;

/**
 * MOMO provider integration scaffold.
 * TODO: implement the real HTTP calls to MOMO's API using credentials from configuration.
 * Example properties to add to application.properties or secrets store:
 *   app.payment.momo.partner-code=
 *   app.payment.momo.access-key=
 *   app.payment.momo.secret-key=
 *   app.payment.momo.return-url=
 *   app.payment.momo.notify-url=
 */
@org.springframework.stereotype.Component("momo")
public class MomoPaymentProvider implements PaymentProvider {
    // TODO: inject config properties for MOMO credentials
    @Override
    public Map<String, String> createCheckout(Payment payment) {
        // TODO: build request body, sign using MOMO secret, call MOMO create order endpoint
        // For now return a mock redirect so developer can test locally.
        Map<String, String> resp = new HashMap<>();
        String providerRef = "MOMO-" + System.currentTimeMillis();
        String redirect = "/mock-provider/momo-checkout?paymentId=" + payment.getId() + "&providerRef=" + providerRef;
        resp.put("redirectUrl", redirect);
        resp.put("providerRef", providerRef);
        return resp;
    }
}
