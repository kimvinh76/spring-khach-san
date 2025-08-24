package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;
import com.example.demo.config.PaymentProperties;
import com.example.demo.model.Payment;

/**
 * Stripe provider integration scaffold.
 */
@org.springframework.stereotype.Component("stripe")
public class StripePaymentProvider implements PaymentProvider {
    private final PaymentProperties props;

    public StripePaymentProvider(PaymentProperties props) {
        this.props = props;
    }

    @Override
    public Map<String, String> createCheckout(Payment payment) {
        // TODO: if props.getStripe().getApiKey() is present, call Stripe Checkout API and return session URL & id
        if (props != null && props.getStripe() != null && props.getStripe().getApiKey() != null) {
            try {
                String apiKey = props.getStripe().getApiKey();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setBasicAuth(apiKey, "");
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
                java.util.Map<String, String> form = new java.util.HashMap<>();
                form.put("payment_method_types[]", "card");
                form.put("mode", "payment");
                form.put("line_items[0][price_data][currency]", "vnd");
                form.put("line_items[0][price_data][product_data][name]", "Booking " + payment.getBookingId());
                form.put("line_items[0][price_data][unit_amount]", String.valueOf(Math.round(payment.getAmount())));
                form.put("line_items[0][quantity]", "1");
                form.put("success_url", "http://localhost:8086/mock-provider/stripe-success?session_id={CHECKOUT_SESSION_ID}");
                form.put("cancel_url", "http://localhost:8086/mock-provider/stripe-cancel");
                String body = form.entrySet().stream().map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, headers);
                org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> resp = rt.postForObject("https://api.stripe.com/v1/checkout/sessions", entity, java.util.Map.class);
                if (resp != null && resp.get("url") != null) {
                    Map<String, String> out = new HashMap<>();
                    out.put("redirectUrl", String.valueOf(resp.get("url")));
                    out.put("providerRef", String.valueOf(resp.get("id")));
                    return out;
                }
            } catch (org.springframework.web.client.RestClientException ex) {
                // fallback to mock
            }
        }
        Map<String, String> resp = new HashMap<>();
        String providerRef = "STRIPE-" + System.currentTimeMillis();
        String redirect = "/mock-provider/stripe-checkout?paymentId=" + payment.getId() + "&providerRef=" + providerRef;
        resp.put("redirectUrl", redirect);
        resp.put("providerRef", providerRef);
        return resp;
    }
}
