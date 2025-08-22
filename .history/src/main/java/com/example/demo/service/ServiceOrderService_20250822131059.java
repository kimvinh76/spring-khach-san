package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.ServiceOrder;
import com.example.demo.repository.ServiceOrderRepository;

@Service
public class ServiceOrderService {
    // Danh sách dịch vụ mẫu (cứng)
    private static final List<String> SERVICE_NAMES = List.of(
        "Spa & Massage", "Giặt ủi", "Đưa đón sân bay", "Tour du lịch", "Ăn uống", "Gym & Pool"
    );
    private static final List<String> SERVICE_DESCRIPTIONS = List.of(
        "Thư giãn, chăm sóc sức khỏe", "Dịch vụ giặt ủi quần áo", "Đưa đón khách tại sân bay", "Tour tham quan địa phương", "Ăn uống tại khách sạn", "Phòng gym và hồ bơi"
    );
    private static final List<Long> SERVICE_PRICES = List.of(
        500_000L, 100_000L, 800_000L, 1_200_000L, 300_000L, 200_000L
    );
    private static final List<String> SERVICE_UNITS = List.of(
        "gói", "kg", "lượt", "tour", "bữa", "lượt"
    );

    public List<String> getServiceNames() {
        return SERVICE_NAMES;
    }
    public List<String> getServiceDescriptions() {
        return SERVICE_DESCRIPTIONS;
    }
    public List<Long> getServicePrices() {
        return SERVICE_PRICES;
    }
    public List<String> getServiceUnits() {
        return SERVICE_UNITS;
    }
    public List<ServiceOrder> findByCustomerName(String customerName) {
        return serviceOrderRepository.findByCustomerName(customerName);
    }

    public List<ServiceOrder> findByPhone(String phone) {
        return serviceOrderRepository.findByPhone(phone);
    }

    public List<ServiceOrder> findAll() {
        return serviceOrderRepository.findAll();
    }

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    public List<ServiceOrder> getAllServiceOrders() {
        return serviceOrderRepository.findAll();
    }

    public List<ServiceOrder> getServiceOrdersByUserId(Long userId) {
        return serviceOrderRepository.findByUserId(userId);
    }

    public ServiceOrder getServiceOrderById(Long id) {
        return serviceOrderRepository.findById(id).orElse(null);
    }

    public ServiceOrder saveServiceOrder(ServiceOrder serviceOrder) {
        return serviceOrderRepository.save(serviceOrder);
    }

    public void deleteServiceOrder(Long id) {
        serviceOrderRepository.deleteById(id);
    }

    public List<ServiceOrder> getServiceOrdersByStatusAndUser(String status, Long userId) {
        return serviceOrderRepository.findByStatusContainingAndUserId(status, userId);
    }
    
    public List<ServiceOrder> findByStatusContaining(String status) {
        return serviceOrderRepository.findByStatusContaining(status);
    }
    
    public List<ServiceOrder> findByBookingId(Long bookingId) {
        return serviceOrderRepository.findByBookingId(bookingId);
    }
    
    public void deleteById(Long id) {
        serviceOrderRepository.deleteById(id);
    }
    
    public java.util.Optional<ServiceOrder> findById(Long id) {
        return serviceOrderRepository.findById(id);
    }
    
    public ServiceOrder save(ServiceOrder serviceOrder) {
        return serviceOrderRepository.save(serviceOrder);
    }

    // --- New helpers for payments ---
    /**
     * Get all confirmed services by booking ID (regardless of payment status)
     */
    public List<ServiceOrder> findConfirmedByBookingId(Long bookingId) {
        List<ServiceOrder> all = findByBookingId(bookingId);
        if (all == null) return java.util.Collections.emptyList();
        return all.stream()
                .filter(o -> {
                    String st = o.getStatus();
                    return st != null && (
                        st.contains("Đã xác nhận") ||
                        st.contains("Đang xử lý") ||
                        st.contains("Hoàn thành")
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Unpaid = orders without invoice attached.
     * Eligible = status contains 'Đã xác nhận' or 'Hoàn thành'.
     */
    public List<ServiceOrder> findUnpaidConfirmedByBookingId(Long bookingId) {
        List<ServiceOrder> all = findByBookingId(bookingId);
        if (all == null) return java.util.Collections.emptyList();
        return all.stream()
                .filter(o -> o.getInvoice() == null)
                .filter(o -> {
                    String st = o.getStatus();
                    // Eligible when admin has progressed the order beyond PENDING
                    // Support both legacy "Đã xác nhận" and current "Đang xử lý" or "Hoàn thành".
                    return st != null && (
                        st.contains("Đã xác nhận") ||
                        st.contains("Đang xử lý") ||
                        st.contains("Hoàn thành")
                    );
                })
                .collect(Collectors.toList());
    }

    public double calculateUnpaidConfirmedTotal(Long bookingId) {
        return findUnpaidConfirmedByBookingId(bookingId).stream()
                .filter(o -> o.getTotalAmount() != null)
                .mapToDouble(ServiceOrder::getTotalAmount)
                .sum();
    }
}
