package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.RoomType;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    // Có thể thêm các hàm truy vấn custom nếu cần
}
