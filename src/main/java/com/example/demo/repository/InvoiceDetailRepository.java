package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.InvoiceDetail;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {
}
