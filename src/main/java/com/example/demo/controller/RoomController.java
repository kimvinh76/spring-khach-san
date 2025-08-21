package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;

/**
 * Controller cho giao diện web quản lý phòng khách sạn (Thymeleaf)
 */
@Controller
@RequestMapping("/admin/rooms")
public class RoomController {
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public RoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    // ...existing code...
}
