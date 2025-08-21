package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang admin chính, menu quản lý
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping
    public String adminHome(Model model) {
        model.addAttribute("pageTitle", "Bảng điều khiển");
        return "admin/home";
    }

    @GetMapping("/home")
    public String adminHomeAlias(Model model) {
        model.addAttribute("pageTitle", "Bảng điều khiển");
        return "admin/home";
    }
    
    @GetMapping("/test")
    public String adminTest(Model model) {
        model.addAttribute("pageTitle", "Test Admin");
        return "admin/test";
    }
}
