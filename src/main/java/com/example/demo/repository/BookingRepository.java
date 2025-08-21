package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Có thể thêm các hàm truy vấn custom nếu cần
    List<Booking> findByPhone(String phone);
    List<Booking> findByPhoneContaining(String phone);
    List<Booking> findByStatusContaining(String status);
    List<Booking> findByStatusContainingIgnoreCase(String status);
    
    // Tìm booking theo user ID
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUserIdAndStatusContaining(Long userId, String status);
    List<Booking> findByStatusContainingAndUserId(String status, Long userId); // Alternative order
    
    // Tìm booking theo user ID và danh sách các status
    List<Booking> findByUserIdAndStatusIn(Long userId, List<String> statuses);
    
    // Tìm booking theo tên khách hàng
    List<Booking> findByCustomerName(String customerName);
    List<Booking> findByCustomerNameContainingIgnoreCase(String customerName);
}
