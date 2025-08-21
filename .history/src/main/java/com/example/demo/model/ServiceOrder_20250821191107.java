package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ServiceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    private String serviceName; // Tên dịch vụ
    private String serviceType; // Loại dịch vụ
    private String description; // Mô tả dịch vụ
    private Double price; // Giá dịch vụ
    private Integer quantity; // Số lượng
    private LocalDateTime orderTime; // Thời gian đặt

    private String customerName; // Tên khách hàng
    private String email; // Email khách hàng
    private String phone; // Số điện thoại khách hàng
    private String roomName; // Tên phòng
    private Long bookingId; // ID booking liên quan
    private String note; // Ghi chú
    private String status; // Trạng thái: "Chờ xác nhận", "Đang xử lý", "Hoàn thành", "Từ chối"
    private Double totalAmount; // Tổng tiền = price * quantity
    private Long userId; // ID người dùng
    
    // Admin fields for processing (removed adminNote for customer interface)
    private String processedBy; // Username or name of staff who processed
    private LocalDateTime updatedAt; // Last updated timestamp by admin

    // Getter, Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    // Removed adminNote getter/setter for customer interface

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
