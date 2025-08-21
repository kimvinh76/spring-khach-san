package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;

/**
 * REST Controller cho quản lý phòng khách sạn.
 * Cung cấp các API CRUD cho Room.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {
    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Lấy danh sách tất cả phòng
     */
    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    /**
     * Lấy thông tin phòng theo id
     */
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Optional<Room> room = roomService.getRoomById(id);
        return room.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Thêm mới phòng
     */
    @PostMapping
    public Room createRoom(@RequestBody Room room) {
        return roomService.saveRoom(room);
    }

    /**
     * Cập nhật phòng
     */
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        if (!roomService.getRoomById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        room.setId(id);
        return ResponseEntity.ok(roomService.saveRoom(room));
    }

    /**
     * Xóa phòng
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomService.getRoomById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
