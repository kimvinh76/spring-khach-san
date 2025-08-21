package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.model.Room;
import com.example.demo.repository.RoomRepository;

/**
 * Service xử lý logic cho phòng khách sạn.
 * Nơi trung gian giữa Controller và Repository.
 */
@Service
public class RoomService {
    private final RoomRepository roomRepository;

    /**
     * Constructor injection: Spring sẽ tự động inject RoomRepository khi khởi tạo RoomService
     */
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Lấy danh sách tất cả phòng
     */
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Lấy danh sách phòng có sẵn cho khách hàng
     */
    public List<Room> getAvailableRooms() {
        return roomRepository.findAvailableRooms();
    }
    
    /**
     * Lấy danh sách phòng có sẵn theo loại phòng
     */
    public List<Room> getAvailableRoomsByRoomType(Long roomTypeId) {
        return roomRepository.findAvailableRoomsByRoomType(roomTypeId);
    }
    
    /**
     * Lấy danh sách phòng theo trạng thái
     */
    public List<Room> getRoomsByStatus(String status) {
        return roomRepository.findByStatus(status);
    }
    
    /**
     * Lấy danh sách phòng không có sẵn (cho admin)
     */
    public List<Room> getUnavailableRooms() {
        return roomRepository.findUnavailableRooms();
    }
    
    /**
     * Cập nhật trạng thái phòng
     */
    public void updateRoomStatus(Long roomId, String status) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.setStatus(status);
            // Tự động cập nhật available dựa trên status
            room.setAvailable("AVAILABLE".equals(status));
            roomRepository.save(room);
        }
    }

    /**
     * Lấy thông tin phòng theo id
     */
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    /**
     * Thêm hoặc cập nhật phòng
     */
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    /**
     * Xóa phòng theo id
     */
    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}
