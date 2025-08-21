package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entity đại diện cho loại phòng khách sạn
 */
@Entity
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ten_loai")
    private String tenLoai; // Tên loại phòng (Thường, Cao cấp, VIP...)
    @Column(name = "description")
    private String description; // Mô tả loại phòng
    @Column(name = "price_base")
    private Double priceBase; // Giá cơ bản cho loại phòng
    @Column(name = "amenities")
    private String amenities; // Tiện nghi (wifi, TV, ...)
    
    @Column(name = "image")
    private String image; // Đường dẫn hình ảnh loại phòng

    // @OneToMany(mappedBy = "roomType")
    // private List<Room> rooms; // Danh sách phòng thuộc loại này

    // Getter, Setter
    // public List<Room> getRooms() { return rooms; }
    // public void setRooms(List<Room> rooms) { this.rooms = rooms; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenLoai() { return tenLoai; }
    public void setTenLoai(String tenLoai) { this.tenLoai = tenLoai; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPriceBase() { return priceBase; }
    public void setPriceBase(Double priceBase) { this.priceBase = priceBase; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
