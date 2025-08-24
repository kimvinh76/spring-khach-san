package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Payment;
import com.example.demo.repository.PaymentRepository;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {
    private final PaymentRepository paymentRepository;
    private final com.example.demo.service.ServiceOrderService serviceOrderService;
    private final com.example.demo.service.BookingService bookingService;
    private final com.example.demo.repository.InvoiceRepository invoiceRepository;

    public AdminPaymentController(PaymentRepository paymentRepository,
                                  com.example.demo.service.ServiceOrderService serviceOrderService,
                                  com.example.demo.service.BookingService bookingService,
                                  com.example.demo.repository.InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.serviceOrderService = serviceOrderService;
        this.bookingService = bookingService;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<Payment> payments = paymentRepository.findAll();
        model.addAttribute("payments", payments);
        return "admin/payments/list";
    }

    @PostMapping("/{id}/reconcile")
    public String reconcile(@PathVariable Long id) {
        Payment p = paymentRepository.findById(id).orElse(null);
        if (p == null) return "redirect:/admin/payments";
        if (!"PAID".equalsIgnoreCase(p.getStatus())) {
            p.setStatus("PAID");
            paymentRepository.save(p);
            // create invoice and attach service orders like webhook
            Long bookingId = p.getBookingId();
            java.util.List<com.example.demo.model.ServiceOrder> payable = serviceOrderService.findUnpaidConfirmedByBookingId(bookingId);
            com.example.demo.model.Invoice invoice = new com.example.demo.model.Invoice();
            invoice.setCustomerName(bookingService.findById(bookingId).map(com.example.demo.model.Booking::getCustomerName).orElse(""));
            invoice.setCreatedTime(java.time.LocalDateTime.now());
            invoice.setTotalAmount(p.getAmount());
            invoice = invoiceRepository.save(invoice);
            for (com.example.demo.model.ServiceOrder s : payable) {
                s.setInvoice(invoice);
                serviceOrderService.save(s);
            }
        }
        return "redirect:/admin/payments";
    }
}
