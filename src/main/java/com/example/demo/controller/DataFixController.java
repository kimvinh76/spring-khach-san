package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;

/**
 * Tiện ích đồng bộ dữ liệu: đồng bộ cờ available với status.
 * Dùng khi data cũ có cờ available sai hoặc null.
 */
@Controller
@RequestMapping("/admin/datafix")
public class DataFixController {
    private final RoomService roomService;

    public DataFixController(RoomService roomService) {
        this.roomService = roomService;
    }

    // POST /admin/datafix/sync-room-availability
    @PostMapping("/sync-room-availability")
    public String syncRoomAvailability() {
        for (Room room : roomService.getAllRooms()) {
            boolean shouldBeAvailable = "AVAILABLE".equals(room.getStatus());
            if (room.getAvailable() == null || room.getAvailable() != shouldBeAvailable) {
                room.setAvailable(shouldBeAvailable);
                roomService.saveRoom(room);
            }
        }
        return "redirect:/admin/rooms";
    }

    // Chuẩn hóa dữ liệu: chuyển mọi trạng thái 'BOOKED' cũ thành 'OCCUPIED'
    @PostMapping("/normalize-room-status")
    public String normalizeRoomStatus() {
        for (Room room : roomService.getAllRooms()) {
            if (room.getStatus() != null && room.getStatus().equalsIgnoreCase("BOOKED")) {
                room.setStatus("OCCUPIED");
                room.setAvailable(false);
                roomService.saveRoom(room);
            }
        }
        return "redirect:/admin/rooms";
    }
}

