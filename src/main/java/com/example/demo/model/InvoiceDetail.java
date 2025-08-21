package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class InvoiceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    private String roomName; // Tên phòng
    private String roomType; // Loại phòng
    private Double price;    // Giá phòng
    private Integer nights;  // Số đêm
    private Double amount;   // Thành tiền

    // Getter, Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
