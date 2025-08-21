package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;

@Controller
@RequestMapping("/rooms")
public class KhachHangRoomController {
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public KhachHangRoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping("/detail/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id).orElse(null);
        model.addAttribute("room", room);
        return "khachhang/rooms/detail";
    }
}
