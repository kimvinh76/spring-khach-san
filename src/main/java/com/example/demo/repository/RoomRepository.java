package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Room;

/**
 * Repository cho entity Room.
 * Kế thừa JpaRepository để sử dụng các hàm CRUD có sẵn (findAll, save, delete, ...)
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    // Tìm phòng theo trạng thái
    List<Room> findByStatus(String status);
    
    // Tìm phòng có sẵn: dựa trên status, không phụ thuộc cờ available (có thể null)
    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE'")
    List<Room> findAvailableRooms();
    
    // Tìm phòng theo loại phòng và trạng thái có sẵn
    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = 'AVAILABLE'")
    List<Room> findAvailableRoomsByRoomType(@Param("roomTypeId") Long roomTypeId);
    
    // Tìm phòng không có sẵn (để admin quản lý)
    @Query("SELECT r FROM Room r WHERE r.status <> 'AVAILABLE'")
    List<Room> findUnavailableRooms();
}
