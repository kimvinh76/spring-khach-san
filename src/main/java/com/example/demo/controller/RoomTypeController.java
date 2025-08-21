package com.example.demo.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.RoomType;
import com.example.demo.service.RoomTypeService;

@Controller
@RequestMapping("/admin/roomtypes")
public class RoomTypeController {
    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    // Hiển thị danh sách loại phòng
    @GetMapping
    public String listRoomTypes(Model model) {
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        return "admin/roomtypes/list";
    }

    // Hiển thị form thêm loại phòng
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("roomType", new RoomType());
        return "admin/roomtypes/form";
    }

    // Xử lý thêm loại phòng
    @PostMapping
    public String createRoomType(@ModelAttribute RoomType roomType) {
        roomTypeService.saveRoomType(roomType);
        return "redirect:/admin/roomtypes";
    }

    // Hiển thị form chỉnh sửa loại phòng
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<RoomType> roomType = roomTypeService.getRoomTypeById(id);
        if (roomType.isPresent()) {
            model.addAttribute("roomType", roomType.get());
            return "admin/roomtypes/form";
        } else {
            return "redirect:/admin/roomtypes";
        }
    }

    // Xử lý cập nhật loại phòng
    @PostMapping("/update/{id}")
    public String updateRoomType(@PathVariable Long id, @ModelAttribute RoomType roomType) {
        roomType.setId(id);
        roomTypeService.saveRoomType(roomType);
        return "redirect:/admin/roomtypes";
    }

    // Xử lý xóa loại phòng
    @GetMapping("/delete/{id}")
    public String deleteRoomType(@PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
        return "redirect:/admin/roomtypes";
    }
}
