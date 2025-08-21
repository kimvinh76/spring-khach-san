package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Invoice;
import com.example.demo.service.InvoiceService;

@Controller
@RequestMapping("/admin/invoices")
public class InvoiceController {
    @Autowired
    private InvoiceService invoiceService;

    @GetMapping
    public String listInvoices(Model model) {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        model.addAttribute("invoices", invoices);
        return "admin/invoices/list";
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        return "admin/invoices/detail";
    }

    @PostMapping("/delete/{id}")
    public String deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return "redirect:/admin/invoices";
    }
}
