package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Room;
import com.example.demo.model.RoomType;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/rooms")
public class RoomListController {
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public RoomListController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public String listRooms(@RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
                            @RequestParam(value = "minPrice", required = false) Double minPrice,
                            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
                            @RequestParam(value = "available", required = false) String availableStr,
                            @RequestParam(value = "sortBy", required = false, defaultValue = "name") String sortBy,
                            @RequestParam(value = "sortDir", required = false, defaultValue = "asc") String sortDir,
                            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
                            @RequestParam(value = "search", required = false) String search,
                            Model model) {
        
        // Lấy danh sách theo filter trạng thái:
        // - available == null  => TẤT CẢ
        // - available == true  => CHỈ CÒN TRỐNG
        // - available == false => ĐÃ ĐẶT/không còn trống
        // Parse tri-state for available from String to avoid empty-string -> false issues
        Boolean available = null;
        if (availableStr != null && !availableStr.isBlank() && !"all".equalsIgnoreCase(availableStr)) {
            available = Boolean.parseBoolean(availableStr);
        }

        List<Room> allRooms = new ArrayList<>();
        if (available == null) {
            allRooms = roomService.getAllRooms();
        } else if (available) {
            allRooms = roomService.getAvailableRooms();
        } else {
            allRooms = roomService.getUnavailableRooms();
        }
        
        // Áp dụng các bộ lọc khác
        allRooms = allRooms.stream()
                .filter(r -> roomTypeId == null || (r.getRoomType() != null && r.getRoomType().getId().equals(roomTypeId)))
                .filter(r -> minPrice == null || (r.getRoomType() != null && r.getRoomType().getPriceBase() >= minPrice))
                .filter(r -> maxPrice == null || (r.getRoomType() != null && r.getRoomType().getPriceBase() <= maxPrice))
                .filter(r -> search == null || search.trim().isEmpty() || 
                           r.getName().toLowerCase().contains(search.toLowerCase()) ||
                           (r.getRoomType() != null && r.getRoomType().getTenLoai().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        // Áp dụng sorting
        switch (sortBy) {
            case "name" -> allRooms.sort((r1, r2) -> "asc".equals(sortDir) ? 
                r1.getName().compareTo(r2.getName()) : r2.getName().compareTo(r1.getName()));
            case "price" -> allRooms.sort((r1, r2) -> {
                double p1 = r1.getRoomType() != null && r1.getRoomType().getPriceBase() != null ? 
                           r1.getRoomType().getPriceBase() : 0.0;
                double p2 = r2.getRoomType() != null && r2.getRoomType().getPriceBase() != null ? 
                           r2.getRoomType().getPriceBase() : 0.0;
                return "asc".equals(sortDir) ? Double.compare(p1, p2) : Double.compare(p2, p1);
            });
            case "roomType" -> allRooms.sort((r1, r2) -> {
                String t1 = r1.getRoomType() != null ? r1.getRoomType().getTenLoai() : "";
                String t2 = r2.getRoomType() != null ? r2.getRoomType().getTenLoai() : "";
                return "asc".equals(sortDir) ? t1.compareTo(t2) : t2.compareTo(t1);
            });
        }

        // Phân trang an toàn chỉ số
        int totalRooms = allRooms.size();
        int safeSize = Math.max(1, size);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRooms / safeSize));
        int safePage = Math.min(Math.max(1, page), totalPages);
        int fromIndex = Math.max(0, (safePage - 1) * safeSize);
        int toIndex = Math.min(fromIndex + safeSize, totalRooms);
        List<Room> roomsPage = fromIndex < toIndex ? new ArrayList<>(allRooms.subList(fromIndex, toIndex)) : new ArrayList<>();
        
        // Lấy thông tin bổ sung - sử dụng phương thức mới
        List<RoomType> roomTypes = roomTypeService.getAllRoomTypes();
        long totalCount = roomService.getAllRooms().size();
        long availableCount = roomService.getAvailableRooms().size();
        
        // Thống kê giá
        List<Double> prices = roomService.getAllRooms().stream()
            .filter(r -> r.getRoomType() != null)
            .map(r -> r.getRoomType().getPriceBase())
            .collect(Collectors.toList());
        Double priceMin = prices.stream().min(Double::compareTo).orElse(0.0);
        Double priceMax = prices.stream().max(Double::compareTo).orElse(10000000.0);
        
        // Add to model
        model.addAttribute("rooms", roomsPage);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
        
        // Filter parameters
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("available", available);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("search", search);
        
        return "khachhang/rooms/list";
    }

    @PostMapping("/add-to-cart/{roomId}")
    public String addToCart(@PathVariable Long roomId, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Long> cart = (List<Long>) session.getAttribute("cart");
        if (cart == null) cart = new ArrayList<>();
        if (!cart.contains(roomId)) cart.add(roomId);
        session.setAttribute("cart", cart);
        return "redirect:/cart";
    }
    
    // Thêm phòng vào giỏ hàng từ trang chủ
    @PostMapping("/rooms/add-to-cart/{roomId}")
    public String addToCartFromHome(@PathVariable Long roomId, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Long> cart = (List<Long>) session.getAttribute("cart");
        if (cart == null) cart = new ArrayList<>();
        if (!cart.contains(roomId)) cart.add(roomId);
        session.setAttribute("cart", cart);
        return "redirect:/cart";
    }
    
    // Trang chủ
    @GetMapping("/home")
    public String home(Model model) {
        // Lấy các phòng nổi bật (giá cao nhất, mới nhất)
        List<Room> featuredRooms = roomService.getAllRooms().stream()
            .filter(Room::getAvailable)
            .sorted((r1, r2) -> {
                Double p1 = r1.getRoomType() != null && r1.getRoomType().getPriceBase() != null ? 
                           r1.getRoomType().getPriceBase() : 0.0;
                Double p2 = r2.getRoomType() != null && r2.getRoomType().getPriceBase() != null ? 
                           r2.getRoomType().getPriceBase() : 0.0;
                return Double.compare(p2, p1);
            })
            .limit(6)
            .toList();
            
        List<RoomType> roomTypes = roomTypeService.getAllRoomTypes();
        long totalRooms = roomService.getAllRooms().size();
        long availableRooms = roomService.getAllRooms().stream().filter(Room::getAvailable).count();
        
        model.addAttribute("featuredRooms", featuredRooms);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("availableRooms", availableRooms);
        
        return "khachhang/home";
    }
}
