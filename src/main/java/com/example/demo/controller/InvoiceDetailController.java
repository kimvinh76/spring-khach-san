package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.InvoiceDetail;
import com.example.demo.service.InvoiceDetailService;

@Controller
@RequestMapping("/admin/invoice-details")
public class InvoiceDetailController {
    private final InvoiceDetailService invoiceDetailService;

    public InvoiceDetailController(InvoiceDetailService invoiceDetailService) {
        this.invoiceDetailService = invoiceDetailService;
    }

    @GetMapping
    public String listInvoiceDetails(Model model) {
        List<InvoiceDetail> details = invoiceDetailService.getAllInvoiceDetails();
        model.addAttribute("details", details);
        return "admin/invoicedetails/list";
    }

    @GetMapping("/{id}")
    public String viewInvoiceDetail(@PathVariable Long id, Model model) {
        InvoiceDetail detail = invoiceDetailService.getInvoiceDetailById(id);
        model.addAttribute("detail", detail);
        return "admin/invoicedetails/detail";
    }

    @PostMapping("/delete/{id}")
    public String deleteInvoiceDetail(@PathVariable Long id) {
        invoiceDetailService.deleteInvoiceDetail(id);
        return "redirect:/admin/invoice-details";
    }
}
