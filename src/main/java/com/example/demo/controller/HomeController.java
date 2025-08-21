package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.RoomType;
import com.example.demo.service.RoomTypeService;

@Controller
public class HomeController {
    private final RoomTypeService roomTypeService;

    public HomeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    // Trang chủ khách hàng
    @GetMapping({"/", "/home", "/khachhang/home"})
    public String home(Model model, @RequestParam(value = "totalPrice", required = false) Long totalPrice) {
        try {
            var roomTypes = roomTypeService.getAllRoomTypes();
            model.addAttribute("roomTypes", roomTypes != null ? roomTypes : java.util.Collections.emptyList());
            model.addAttribute("noRoomType", roomTypes == null || roomTypes.isEmpty());
            if (totalPrice != null) {
                model.addAttribute("totalPrice", totalPrice);
            }
            return "khachhang/home";
        } catch (Exception e) {
            // Log lỗi và trả về trang với thông báo lỗi
            model.addAttribute("roomTypes", java.util.Collections.emptyList());
            model.addAttribute("noRoomType", true);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải dữ liệu");
            return "khachhang/home";
        }
    }

    // Tính tiền nhanh
    @GetMapping("/tinh-tien")
    public String tinhTien(@RequestParam Long roomTypeId, @RequestParam int nights, Model model) {
        try {
            RoomType roomType = roomTypeService.getRoomTypeById(roomTypeId).orElse(null);
            long totalPrice = 0;
            if (roomType != null && roomType.getPriceBase() != null && nights > 0) {
                totalPrice = (long) (roomType.getPriceBase() * nights);
            }
            var roomTypes = roomTypeService.getAllRoomTypes();
            model.addAttribute("roomTypes", roomTypes != null ? roomTypes : java.util.Collections.emptyList());
            model.addAttribute("noRoomType", roomTypes == null || roomTypes.isEmpty());
            model.addAttribute("totalPrice", totalPrice);
            return "khachhang/home";
        } catch (Exception e) {
            // Nếu có lỗi, quay về trang chủ với thông báo lỗi
            model.addAttribute("roomTypes", java.util.Collections.emptyList());
            model.addAttribute("noRoomType", true);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tính tiền");
            return "khachhang/home";
        }
    }
}
