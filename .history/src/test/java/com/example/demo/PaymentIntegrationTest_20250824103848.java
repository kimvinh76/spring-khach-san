package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import com.example.demo.model.Payment;
import com.example.demo.service.BookingService;
import com.example.demo.service.PaymentService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    BookingService bookingService;

    @Autowired
    PaymentService paymentService;

    @Test
    void endToEndMockPaymentFlow() {
        // Create a booking using BookingService helper (assumes BookingService has convenience method)
        var booking = bookingService.createSampleBookingForTest();
        assertThat(booking).isNotNull();

        // Initiate payment via API
        Map<String, Object> req = Map.of("bookingId", booking.getId(), "amount", 100000.0, "provider", "mock");
        ResponseEntity<Map> resp = rest.postForEntity("/api/payments/initiate", req, Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Map body = resp.getBody();
        assertThat(body).containsKey("redirectUrl");
        assertThat(body).containsKey("paymentId");

        Integer paymentId = (Integer) body.get("paymentId");
        String redirect = (String) body.get("redirectUrl");

        // Simulate provider completion by POST to /mock-provider/complete
        var form = Map.of("paymentId", paymentId.longValue(), "providerRef", "MOCK-TEST");
        ResponseEntity<String> complete = rest.postForEntity("/mock-provider/complete", form, String.class);
        assertThat(complete.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify payment status
        Payment p = paymentService.findById(paymentId.longValue()).orElse(null);
        assertThat(p).isNotNull();
        assertThat(p.getStatus()).isEqualToIgnoringCase("PAID");
    }
}
