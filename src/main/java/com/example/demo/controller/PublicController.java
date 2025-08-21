// package com.example.demo.controller;

// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;

// import com.example.demo.service.RoomTypeService;

// @Controller
// public class PublicController {
    
//     private final RoomTypeService roomTypeService;
    
//     public PublicController(RoomTypeService roomTypeService) {
//         this.roomTypeService = roomTypeService;
//     }
    
//     @GetMapping("/room-types")
//     public String roomTypes(Model model) {
//         model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
//         return "khachhang/room-types";
//     }
    
//     @GetMapping("/contact")
//     public String contact() {
//         return "khachhang/contact";
//     }
// }
