package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Booking;
import com.example.demo.model.ServiceOrder;
import com.example.demo.model.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.ServiceOrderService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/legacy-services")
public class ServiceController {
    
    private static final String ERROR_ATTR = "error";
    private static final String SUCCESS_ATTR = "success";
    
    private final ServiceOrderService serviceOrderService;
    private final BookingService bookingService;

    public ServiceController(ServiceOrderService serviceOrderService, BookingService bookingService) {
        this.serviceOrderService = serviceOrderService;
        this.bookingService = bookingService;
    }

    // Trang chính dịch vụ
    @GetMapping("/menu")
    public String serviceMenu() {
        return "khachhang/services/index";
    }
    
    // Trang chính dịch vụ - route chính
    @GetMapping
    public String servicesIndex() {
        return "khachhang/services/index";
    }
    
    // Danh sách dịch vụ có thể đặt cho một booking
    @GetMapping("/list")
    public String serviceList(@RequestParam(required = false) Long bookingId, 
                             HttpSession session,
                             @RequestParam(required = false) String phone,
                             Model model) {
        // Lấy user từ session
        User currentUser = (User) session.getAttribute("currentUser");
        Long userId = null;
        String username = null;
        
        if (currentUser != null) {
            userId = currentUser.getId();
            username = currentUser.getUsername();
        } else {
            // Fallback: lấy từ session cũ
            username = (String) session.getAttribute("username");
            userId = (Long) session.getAttribute("userId");
        }
        
        List<Booking> userBookings = userId != null ? bookingService.findByUserId(userId) : new java.util.ArrayList<>();
        // Kiểm tra có booking ID không
        if (bookingId != null) {
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            if (booking != null && (userId == null || booking.getUserId().equals(userId))) {
                boolean canOrderService = booking.getStatus() != null && 
                    ("Đã xác nhận".equals(booking.getStatus()) || 
                     booking.getStatus().toLowerCase().contains("thanh toán"));
                model.addAttribute("selectedBooking", booking);
                model.addAttribute("bookingId", bookingId);
                model.addAttribute("canOrderService", canOrderService);
                if (canOrderService) {
                    List<Booking> validBookings = new java.util.ArrayList<>();
                    validBookings.add(booking);
                    model.addAttribute("bookings", validBookings);
                    
                    // Thêm danh sách dịch vụ và giá tiền
                    addServicesData(model);
                }
            }
        } else {
            // Hiển thị tất cả booking hợp lệ của user
            List<Booking> validBookings = new java.util.ArrayList<>();
            for (Booking booking : userBookings) {
                if (booking.getStatus() != null && ("Đã xác nhận".equals(booking.getStatus()) || booking.getStatus().toLowerCase().contains("thanh toán"))) {
                    validBookings.add(booking);
                }
            }
            model.addAttribute("bookings", validBookings);
        }
        
        // Luôn thêm danh sách dịch vụ để template có thể hiển thị
        addServicesData(model);
        
        return "khachhang/services/list";
    }
    
    // Đặt dịch vụ
    @PostMapping("/order")
    public String orderService(@RequestParam Long bookingId,
                              @RequestParam String serviceName,
                              @RequestParam Double servicePrice,
                              @RequestParam Integer quantity,
                              @RequestParam(required = false) String note,
                              RedirectAttributes redirectAttributes) {
        
        try {
            // Lấy thông tin booking
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute(ERROR_ATTR, "Không tìm thấy thông tin booking!");
                return "redirect:/khachhang/services/list";
            }
            
                // Chỉ cho đặt dịch vụ nếu booking đã xác nhận hoặc đã thanh toán
                if (!"Đã xác nhận".equals(booking.getStatus()) && 
                    (booking.getStatus() == null || !booking.getStatus().toLowerCase().contains("thanh toán"))) {
                    redirectAttributes.addFlashAttribute(ERROR_ATTR, 
                        "Chỉ có thể đặt dịch vụ cho booking đã được admin xác nhận hoặc đã thanh toán! Trạng thái hiện tại: " + booking.getStatus());
                    return "redirect:/khachhang/services/list?bookingId=" + bookingId;
                }
            
            ServiceOrder serviceOrder = new ServiceOrder();
            serviceOrder.setServiceName(serviceName);
            serviceOrder.setServiceType(serviceName);
            serviceOrder.setPrice(servicePrice);
            serviceOrder.setQuantity(quantity);
            serviceOrder.setTotalAmount(servicePrice * quantity);
            serviceOrder.setStatus("Chờ xác nhận");
            serviceOrder.setOrderTime(LocalDateTime.now());
            serviceOrder.setCustomerName(booking.getCustomerName());
            serviceOrder.setEmail(booking.getEmail());
            serviceOrder.setPhone(booking.getPhone());
            serviceOrder.setRoomName(booking.getRoom() != null ? booking.getRoom().getName() : "");
            serviceOrder.setBookingId(booking.getId());
                serviceOrder.setUserId(booking.getUserId());
            serviceOrder.setNote(note);
            serviceOrder.setDescription(note != null ? note : "Dịch vụ " + serviceName);
            
            serviceOrderService.saveServiceOrder(serviceOrder);
            
            redirectAttributes.addFlashAttribute(SUCCESS_ATTR, 
                "Đặt dịch vụ " + serviceName + " thành công! Tổng tiền: " + 
                String.format("%,.0f", serviceOrder.getTotalAmount()) + " VNĐ");
            
            return "redirect:/khachhang/services/my-orders?bookingId=" + booking.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_ATTR, "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/khachhang/services/list";
        }
    }
    
    // Thêm dữ liệu dịch vụ vào model
    private void addServicesData(Model model) {
        // Danh sách dịch vụ cao cấp khách sạn
        List<String> services = List.of(
            "Spa & Massage",
            "Giặt ủi",  
            "Đưa đón sân bay",
            "Tour du lịch",
            "Ăn uống",
            "Gym & Pool"
        );
        
        // Mô tả dịch vụ
        List<String> descriptions = List.of(
            "Thư giãn với massage chuyên nghiệp và spa cao cấp",
            "Dịch vụ giặt ủi nhanh chóng, chất lượng cao",
            "Đưa đón sân bay 24/7 với xe sang trọng",
            "Tour du lịch khám phá thành phố và vùng lân cận",
            "Ăn uống tại nhà hàng 5 sao với món ngon đa dạng",
            "Tập gym và bơi lội với trang thiết bị hiện đại"
        );
        
        // Giá dịch vụ (VNĐ)
        List<Double> prices = List.of(
            500000.0,   // Spa & Massage
            100000.0,   // Giặt ủi
            800000.0,   // Đưa đón sân bay
            1200000.0,  // Tour du lịch
            300000.0,   // Ăn uống
            200000.0    // Gym & Pool
        );
        
        // Đơn vị tính
        List<String> units = List.of(
            "buổi",     // Spa & Massage
            "kg",       // Giặt ủi
            "chuyến",   // Đưa đón sân bay
            "tour",     // Tour du lịch
            "món",      // Ăn uống
            "ngày"      // Gym & Pool
        );
        
        model.addAttribute("services", services);
        model.addAttribute("descriptions", descriptions);
        model.addAttribute("prices", prices);
        model.addAttribute("units", units);
    }
}
