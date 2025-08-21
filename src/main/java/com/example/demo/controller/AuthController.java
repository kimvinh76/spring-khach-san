package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.User;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    // Trang đăng nhập
    @GetMapping("/login")
    public String loginPage(Model model) {
        return "auth/login";
    }
    
    // Xử lý đăng nhập
    @PostMapping("/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        try {
            var user = userService.loginUser(username, password);
            if (user.isPresent()) {
                // Set cả currentUser và username để tương thích với các controller khác
                session.setAttribute("currentUser", user.get());
                session.setAttribute("username", user.get().getUsername());
                session.setAttribute("userId", user.get().getId());
                redirectAttributes.addFlashAttribute("success", "Đăng nhập thành công! Chào mừng " + user.get().getFullName());
                
                // Redirect dựa trên role
                if ("ADMIN".equals(user.get().getRole())) {
                    return "redirect:/admin";
                } else {
                    return "redirect:/khachhang/home";
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
                return "redirect:/auth/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/auth/login";
        }
    }
    
    // Trang đăng ký
    @GetMapping("/register")
    public String registerPage(Model model) {
        return "auth/register";
    }
    
    // Xử lý đăng ký
    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam String fullName,
                          @RequestParam String email,
                          @RequestParam String phone,
                          RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra mật khẩu khớp
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
                return "redirect:/auth/register";
            }
            
            // Đăng ký user
            User newUser = userService.registerUser(username, password, fullName, email, phone);
            redirectAttributes.addFlashAttribute("success", 
                "Đăng ký thành công! Chào mừng " + newUser.getFullName() + " đến với hệ thống.");
            return "redirect:/auth/login";
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi đăng ký: " + e.getMessage());
            return "redirect:/auth/register";
        }
    }
    
    // Đăng xuất
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đăng xuất thành công!");
        return "redirect:/auth/login";
    }
    
    // Check username availability (AJAX)
    @GetMapping("/check-username")
    public String checkUsername(@RequestParam String username, Model model) {
        boolean exists = userService.isUsernameExists(username);
        model.addAttribute("exists", exists);
        return "fragments/username-check";
    }
}
