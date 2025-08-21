package com.example.demo.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {
    private static final String REDIRECT_ADMIN_ROOMS = "redirect:/admin/rooms";
    
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public AdminRoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    // Hiển thị danh sách phòng
    @GetMapping
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "admin/rooms/list";
    }

    // Hiển thị form thêm phòng
    @GetMapping("/add")
    public String showCreateForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        return "admin/rooms/form";
    }

    // Xử lý thêm phòng
    @PostMapping
    public String createRoom(@ModelAttribute Room room) {
        roomService.saveRoom(room);
        return REDIRECT_ADMIN_ROOMS;
    }

    // Hiển thị form chỉnh sửa phòng
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Room> room = roomService.getRoomById(id);
        if (room.isPresent()) {
            model.addAttribute("room", room.get());
            model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
            return "admin/rooms/form";
        } else {
            return REDIRECT_ADMIN_ROOMS;
        }
    }

    // Xử lý cập nhật phòng
    @PostMapping("/update/{id}")
    public String updateRoom(@PathVariable Long id, @ModelAttribute Room room) {
        room.setId(id);
        roomService.saveRoom(room);
        return REDIRECT_ADMIN_ROOMS;
    }

    // Xử lý xóa phòng
    @GetMapping("/delete/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return REDIRECT_ADMIN_ROOMS;
    }
    
    // Cập nhật trạng thái phòng
    @PostMapping("/update-status/{id}")
    public String updateRoomStatus(@PathVariable Long id, 
                                   @RequestParam String status) {
        roomService.updateRoomStatus(id, status);
        return REDIRECT_ADMIN_ROOMS;
    }
}
