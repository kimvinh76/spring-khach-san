package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.model.InvoiceDetail;
import com.example.demo.repository.InvoiceDetailRepository;

@Service
public class InvoiceDetailService {
    private final InvoiceDetailRepository invoiceDetailRepository;

    public InvoiceDetailService(InvoiceDetailRepository invoiceDetailRepository) {
        this.invoiceDetailRepository = invoiceDetailRepository;
    }

    public List<InvoiceDetail> getAllInvoiceDetails() {
        return invoiceDetailRepository.findAll();
    }

    public InvoiceDetail getInvoiceDetailById(Long id) {
        return invoiceDetailRepository.findById(id).orElse(null);
    }

    public InvoiceDetail saveInvoiceDetail(InvoiceDetail detail) {
        return invoiceDetailRepository.save(detail);
    }

    public void deleteInvoiceDetail(Long id) {
        invoiceDetailRepository.deleteById(id);
    }
}
