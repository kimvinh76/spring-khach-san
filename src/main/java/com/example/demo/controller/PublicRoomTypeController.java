package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.service.RoomTypeService;

@Controller
@RequestMapping("/roomtypes")
public class PublicRoomTypeController {
    private final RoomTypeService roomTypeService;

    public PublicRoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public String listRoomTypes(Model model) {
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        return "roomtypes/list";
    }
}
