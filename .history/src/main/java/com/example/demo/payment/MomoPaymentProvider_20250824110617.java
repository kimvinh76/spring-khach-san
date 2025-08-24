package com.example.demo.payment;

import java.util.HashMap;
import java.util.Map;

import com.example.demo.config.PaymentProperties;
import com.example.demo.model.Payment;

/**
 * MOMO provider integration scaffold.
 * NOTE: implement the real HTTP calls to MOMO's API using credentials from configuration.
 * Example properties to add to application.properties or secrets store:
 *   app.payment.momo.partner-code=
 *   app.payment.momo.access-key=
 *   app.payment.momo.secret-key=
 *   app.payment.momo.return-url=
 *   app.payment.momo.notify-url=
 */
@org.springframework.stereotype.Component("momo")
public class MomoPaymentProvider implements PaymentProvider {
    private final PaymentProperties props;

    public MomoPaymentProvider(PaymentProperties props) {
        this.props = props;
    }
    @Override
    public Map<String, String> createCheckout(Payment payment) {
        // If MOMO is configured with endpoint and keys, call real API (TODO: implement signature and request per MOMO docs)
        if (props != null && props.getMomo() != null && props.getMomo().getEndpoint() != null) {
            try {
                var momo = props.getMomo();
                String requestId = java.util.UUID.randomUUID().toString();
                String orderId = "ORDER-" + java.util.UUID.randomUUID().toString();
                String amountStr = String.valueOf(Math.round(payment.getAmount()));
                String orderInfo = "Thanh toan booking " + payment.getBookingId();
                String extraData = "";

                // Build raw signature string according to MOMO docs (field order matters)
                String raw = "partnerCode=" + momo.getPartnerCode()
                        + "&accessKey=" + momo.getAccessKey()
                        + "&requestId=" + requestId
                        + "&amount=" + amountStr
                        + "&orderId=" + orderId
                        + "&orderInfo=" + orderInfo
                        + "&returnUrl=" + momo.getReturnUrl()
                        + "&notifyUrl=" + momo.getNotifyUrl()
                        + "&extraData=" + extraData;

                String signature = computeHmacSha256Hex(raw, momo.getSecretKey());

                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("partnerCode", momo.getPartnerCode());
                payload.put("accessKey", momo.getAccessKey());
                payload.put("requestId", requestId);
                payload.put("amount", amountStr);
                payload.put("orderId", orderId);
                payload.put("orderInfo", orderInfo);
                payload.put("returnUrl", momo.getReturnUrl());
                payload.put("notifyUrl", momo.getNotifyUrl());
                payload.put("extraData", extraData);
                payload.put("requestType", "captureWallet");
                payload.put("signature", signature);

                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(payload, headers);
                org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> resp = rt.postForObject(momo.getEndpoint(), entity, java.util.Map.class);
                if (resp != null) {
                    // typical keys: payUrl or payUrl; momo may return 'payUrl' or 'payUrl' depending on env
                    Object payUrl = resp.get("payUrl");
                    if (payUrl == null) payUrl = resp.get("payUrl");
                    String redirectUrl = payUrl == null ? null : String.valueOf(payUrl);
                    String providerRef = resp.getOrDefault("orderId", orderId).toString();
                    if (redirectUrl != null) {
                        Map<String, String> out = new HashMap<>();
                        out.put("redirectUrl", redirectUrl);
                        out.put("providerRef", providerRef);
                        return out;
                    }
                }
            } catch (java.security.GeneralSecurityException | org.springframework.web.client.RestClientException ex) {
                // on any error we'll fall back to mock behavior below
            }
        }

        // Fallback/mock behavior
        Map<String, String> resp = new HashMap<>();
        String providerRef = "MOMO-" + System.currentTimeMillis();
        String redirect = "/mock-provider/momo-checkout?paymentId=" + payment.getId() + "&providerRef=" + providerRef;
        resp.put("redirectUrl", redirect);
        resp.put("providerRef", providerRef);
        return resp;
    }

    /**
     * Compute HMAC-SHA256 hex digest for the given data and key.
     */
    private static String computeHmacSha256Hex(String data, String key) throws java.security.GeneralSecurityException {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
