package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Entity đại diện cho một phòng trong khách sạn.
 * Chú thích @Entity giúp Spring Boot ánh xạ class này với bảng trong database.
 */
@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Mã phòng (tự tăng)

    @Column(name = "name")
    private String name; // Mã phòng hoặc số phòng (ví dụ: DL304, DLL210)
    
    @ManyToOne
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType; // Loại phòng (liên kết với RoomType)
    
    @Column(name = "capacity")
    private Integer capacity; // Sức chứa tối đa
    
    @Column(name = "description")
    private String description; // Mô tả phòng
    
    @Column(name = "image")
    private String image; // Đường dẫn hoặc tên file hình ảnh
    
    @Column(name = "available")
    private Boolean available; // Trạng thái phòng (còn trống hay không)
    
    @Column(name = "status")
    private String status = "AVAILABLE"; // Trạng thái: AVAILABLE, OCCUPIED, MAINTENANCE
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getter, Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    // Getter cho roomNumber (alias cho name để compatibility với template)
    public String getRoomNumber() { return name; }
    public void setRoomNumber(String roomNumber) { this.name = roomNumber; }
    
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
