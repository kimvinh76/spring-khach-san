package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.service.RoomTypeService;

@Controller
public class KhachHangPageController {
    
    private final RoomTypeService roomTypeService;
    
    public KhachHangPageController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }
    
    @GetMapping("/room-types")
    public String roomTypes(Model model) {
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        return "khachhang/room-types";
    }

    @GetMapping("/services-old")
    public String services() {
        return "khachhang/services";
    }

    @GetMapping("/about")
    public String about() {
        return "khachhang/about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "khachhang/contact";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "khachhang/checkout";
    }
}
