package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Invoice;
import com.example.demo.model.Payment;
import com.example.demo.model.ServiceOrder;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.PaymentService;
import com.example.demo.service.ServiceOrderService;

@Controller
@RequestMapping("/mock-provider")
public class MockProviderController {
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final ServiceOrderService serviceOrderService;
    private final InvoiceRepository invoiceRepository;

    public MockProviderController(PaymentService paymentService,
                                  BookingService bookingService,
                                  ServiceOrderService serviceOrderService,
                                  InvoiceRepository invoiceRepository) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.serviceOrderService = serviceOrderService;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam Long paymentId, @RequestParam String providerRef, Model model) {
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("providerRef", providerRef);
        return "mock/provider-checkout";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam Long paymentId, @RequestParam String providerRef, Model model) {
        Payment p = paymentService.findById(paymentId).orElse(null);
        if (p == null) {
            model.addAttribute("error", "payment not found");
            return "mock/provider-complete";
        }

        // idempotent: if already PAID, just show success
        if ("PAID".equalsIgnoreCase(p.getStatus())) {
            model.addAttribute("payment", p);
            return "khachhang/payment/success";
        }

        p.setProviderRef(providerRef == null ? String.valueOf(p.getId()) : providerRef);
        p.setStatus("PAID");
        paymentService.save(p);

        // Create invoice and attach service orders
        Long bookingId = p.getBookingId();
        List<ServiceOrder> payable = serviceOrderService.findUnpaidConfirmedByBookingId(bookingId);

    Invoice invoice = new Invoice();
    invoice.setCustomerName(bookingService.findById(bookingId).map(com.example.demo.model.Booking::getCustomerName).orElse(""));
        invoice.setCreatedTime(LocalDateTime.now());
        invoice.setTotalAmount(p.getAmount());
        invoice.setNote("Mock provider completion: " + providerRef);
        invoice = invoiceRepository.save(invoice);

        for (ServiceOrder s : payable) {
            s.setInvoice(invoice);
            serviceOrderService.save(s);
        }

        // mark booking as paid
        bookingService.findById(bookingId).ifPresent(b -> {
            String status = b.getStatus() == null ? "" : b.getStatus();
            if (!status.contains("Đã thanh toán")) {
                b.setStatus((status.isEmpty() ? "" : status + " - ") + "Đã thanh toán");
                bookingService.save(b);
            }
        });

        model.addAttribute("payment", p);
        return "khachhang/payment/success";
    }
}
