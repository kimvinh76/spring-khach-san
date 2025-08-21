
package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/**
 * Entity Booking: lưu thông tin đặt phòng
 */
@Entity
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room; // Phòng được đặt
    
    private Long userId; // ID của user đặt phòng (để liên kết với User)

    private String customerName; // Tên khách hàng
    private String email; // Email khách hàng
    private String phone; // Số điện thoại
    private int guestCount; // Số lượng khách
    private String note; // Ghi chú đặc biệt
    private LocalDate checkIn; // Ngày nhận phòng
    private LocalDate checkOut; // Ngày trả phòng
    private LocalDateTime bookingDate; // Ngày đặt phòng
    private BigDecimal totalAmount; // Tổng tiền
    private String code; // Mã xác nhận booking
    private String status; // Trạng thái (Đã đặt, Đã hủy, Đã thanh toán...)

    @Transient
    private double serviceTotal; // Tổng tiền dịch vụ (không lưu database)
    
    @Transient
    private int servicesPendingCount; // Số dịch vụ chờ xác nhận (không lưu database)
    
    @Transient
    private int servicesCount; // Tổng số dịch vụ (không lưu database)

    // Getter, Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public double getServiceTotal() { return serviceTotal; }
    public void setServiceTotal(double serviceTotal) { this.serviceTotal = serviceTotal; }
    
    public int getServicesPendingCount() { return servicesPendingCount; }
    public void setServicesPendingCount(int servicesPendingCount) { this.servicesPendingCount = servicesPendingCount; }
    
    public int getServicesCount() { return servicesCount; }
    public void setServicesCount(int servicesCount) { this.servicesCount = servicesCount; }
    
    // Tính số đêm ở
    public long getTotalNights() {
        if (checkIn != null && checkOut != null) {
            return ChronoUnit.DAYS.between(checkIn, checkOut);
        }
        return 0;
    }
}
