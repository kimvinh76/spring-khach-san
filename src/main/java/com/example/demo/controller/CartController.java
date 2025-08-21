package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CartController {
    private final RoomService roomService;

    public CartController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Object cartObj = session.getAttribute("cart");
        List<Long> cart;
        if (cartObj instanceof List<?>) {
            cart = ((List<?>) cartObj).stream()
                .filter(id -> id instanceof Long)
                .map(id -> (Long) id)
                .toList();
        } else {
            cart = new ArrayList<>();
        }
        List<Room> rooms = cart.stream()
                .map(roomService::getRoomById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
        model.addAttribute("rooms", rooms);
        return "khachhang/rooms/cart";
    }

    @PostMapping("/cart/remove/{roomId}")
    public String removeFromCart(@PathVariable Long roomId, HttpSession session) {
        List<Long> cart = (List<Long>) session.getAttribute("cart");
        if (cart != null) {
            cart.remove(roomId);
            session.setAttribute("cart", cart);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout/{roomId}")
    public String checkoutRoom(@PathVariable Long roomId) {
        // Redirect to booking form for the selected room
        return "redirect:/booking?roomId=" + roomId;
    }
}
