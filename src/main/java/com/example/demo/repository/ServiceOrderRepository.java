package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.ServiceOrder;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    List<ServiceOrder> findByCustomerName(String customerName);
    List<ServiceOrder> findByPhone(String phone);
    List<ServiceOrder> findByCustomerNameContainingIgnoreCase(String customerName);
    List<ServiceOrder> findByStatus(String status);
    List<ServiceOrder> findByStatusContaining(String status);
    List<ServiceOrder> findByCustomerNameAndStatus(String customerName, String status);
    List<ServiceOrder> findByBookingId(Long bookingId);
    
    // User-specific methods
    List<ServiceOrder> findByUserId(Long userId);
    List<ServiceOrder> findByStatusContainingAndUserId(String status, Long userId);
}
