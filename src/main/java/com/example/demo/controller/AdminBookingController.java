package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Booking;
import com.example.demo.model.Room;
import com.example.demo.model.ServiceOrder;
import com.example.demo.service.BookingService;
import com.example.demo.service.RoomService;
import com.example.demo.service.ServiceOrderService;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {
    private final BookingService bookingService;
    private final RoomService roomService;
    private final ServiceOrderService serviceOrderService;

    private static final String BOOKING_STATUS_PENDING = "Chờ xác nhận";
    private static final String BOOKING_STATUS_CONFIRMED = "Đã xác nhận";
    private static final String BOOKING_STATUS_REJECTED = "Từ chối";
    private static final String STATUS_CONFIRMED = "Đã xác nhận";
    private static final String STATUS_PROCESSING = "Đang xử lý";
    private static final String REDIRECT_BOOKINGS = "redirect:/admin/bookings";
    private static final String FLASH_SUCCESS = "success";
    private static final String FLASH_ERROR = "error";
    private static final String MSG_NOT_FOUND = "Không tìm thấy booking!";

    public AdminBookingController(BookingService bookingService, RoomService roomService, 
                                 ServiceOrderService serviceOrderService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.serviceOrderService = serviceOrderService;
    }

    // Liệt kê tất cả booking với phân trang và lọc
    @GetMapping
    public String listBookings(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "") String status, 
                              Model model) {
        
        // Giới hạn kích thước trang
        if (size > 50) size = 50;
        if (size < 5) size = 5;
        
        List<Booking> allBookings;
        if (!status.isEmpty()) {
            allBookings = bookingService.findByStatusContaining(status);
        } else {
            allBookings = bookingService.findAll();
        }
        
        // Sắp xếp theo ID giảm dần (booking mới nhất trước)
        allBookings.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        
        // Tính toán phân trang thủ công
        int totalBookings = allBookings.size();
        int totalPages = (int) Math.ceil((double) totalBookings / size);
        
        // Đảm bảo page hợp lệ
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;
        
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalBookings);
        
        List<Booking> bookings = startIndex < totalBookings ? 
            allBookings.subList(startIndex, endIndex) : new ArrayList<>();
        
        // Thống kê luôn tính trên toàn bộ dữ liệu (không phân trang)
        List<Booking> statsBookings = bookingService.findAll();
        long pendingCount = statsBookings.stream().filter(b -> BOOKING_STATUS_PENDING.equals(b.getStatus())).count();
        long confirmedCount = statsBookings.stream().filter(b -> BOOKING_STATUS_CONFIRMED.equals(b.getStatus())).count();
        long rejectedCount = statsBookings.stream().filter(b -> b.getStatus() != null && b.getStatus().contains(BOOKING_STATUS_REJECTED)).count();
        
        double totalRevenue = statsBookings.stream()
            .filter(booking -> booking.getTotalAmount() != null)
            .filter(booking -> !booking.getStatus().contains(BOOKING_STATUS_REJECTED))
            .mapToDouble(booking -> booking.getTotalAmount().doubleValue())
            .sum();
        
        // Thêm thông tin dịch vụ cho từng booking
        for (Booking booking : bookings) {
            // Tính tiền phòng
            if (booking.getCheckIn() != null && booking.getCheckOut() != null && booking.getRoom() != null) {
                long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
                if (nights < 1) nights = 1;
                Double priceBase = booking.getRoom().getRoomType().getPriceBase();
                if (priceBase != null) {
                    booking.setTotalAmount(java.math.BigDecimal.valueOf(nights * priceBase.longValue()));
                }
            }

            // Tính tổng tiền dịch vụ
            List<ServiceOrder> services = serviceOrderService.findByBookingId(booking.getId());
            double serviceTotal = services.stream()
                .filter(o -> o.getTotalAmount() != null)
                .filter(o -> !"Từ chối".equals(o.getStatus()))
                .mapToDouble(ServiceOrder::getTotalAmount)
                .sum();
            booking.setServiceTotal(serviceTotal);
            booking.setServicesCount(services.size());

            long pendingServices = services.stream()
                .filter(s -> BOOKING_STATUS_PENDING.equals(s.getStatus()))
                .count();
            booking.setServicesPendingCount((int) pendingServices);
        }
        
        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalRevenue", totalRevenue);
        
        // Thông tin phân trang
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("hasPrevious", page > 0);
        
        return "admin/bookings/list";
    }

    // Xem chi tiết booking
    @GetMapping("/{id}")
    public String viewBookingDetail(@PathVariable Long id, Model model) {
        try {
            Booking booking = bookingService.findById(id).orElse(null);
            if (booking == null) {
                model.addAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_BOOKINGS;
            }
            
            // Tính tiền phòng và đảm bảo luôn có giá trị
            if (booking.getCheckIn() != null && booking.getCheckOut() != null && booking.getRoom() != null) {
                long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
                if (nights < 1) nights = 1;
                
                Double priceBase = booking.getRoom().getRoomType() != null ? 
                    booking.getRoom().getRoomType().getPriceBase() : null;
                
                if (priceBase != null && priceBase > 0) {
                    booking.setTotalAmount(java.math.BigDecimal.valueOf(nights * priceBase.longValue()));
                } else {
                    // Nếu không có giá, set default
                    booking.setTotalAmount(java.math.BigDecimal.ZERO);
                }
                
                // Lưu lại vào database
                bookingService.save(booking);
            } else {
                // Đảm bảo luôn có totalAmount
                if (booking.getTotalAmount() == null) {
                    booking.setTotalAmount(java.math.BigDecimal.ZERO);
                    bookingService.save(booking);
                }
            }
            
            // Lấy dịch vụ liên quan
            List<ServiceOrder> services = serviceOrderService.findByBookingId(id);
            double serviceTotal = services.stream()
                .filter(o -> o.getTotalAmount() != null)
                .filter(o -> !"Từ chối".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalAmount())
                .sum();
            booking.setServiceTotal(serviceTotal);

            // Ensure template-friendly numeric attributes are present so the view
            // can render totals without showing 'null'.
            long roomTotal = booking.getTotalAmount() != null ? booking.getTotalAmount().longValue() : 0L;
            long serviceTotalLong = (long) serviceTotal;
            long total = roomTotal + serviceTotalLong;

            model.addAttribute("booking", booking);
            model.addAttribute("services", services);
            model.addAttribute("roomTotal", roomTotal);
            model.addAttribute("serviceTotal", serviceTotalLong);
            model.addAttribute("total", total);

            // Debug logs to help trace why roomTotal might render as null in the view
            System.out.println("[DEBUG] booking.id=" + booking.getId() + ", booking.totalAmount=" + booking.getTotalAmount());
            System.out.println("[DEBUG] roomTotal=" + roomTotal + ", serviceTotal=" + serviceTotalLong + ", total=" + total);
            
            return "admin/bookings/detail";
            
        } catch (Exception e) {
            model.addAttribute(FLASH_ERROR, "Lỗi khi xem chi tiết booking: " + e.getMessage());
            return REDIRECT_BOOKINGS;
        }
    }

    // Xác nhận booking (CHỈ XÁC NHẬN BOOKING, KHÔNG XÁC NHẬN DỊCH VỤ)
    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_BOOKINGS;
            }
            
            booking.setStatus(STATUS_CONFIRMED);
            
            // Cập nhật trạng thái phòng
            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(false);
                room.setStatus("BOOKED");
                roomService.saveRoom(room);
            }
            
            bookingService.save(booking);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                "Đã xác nhận booking #" + id + " cho khách hàng " + booking.getCustomerName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi xác nhận booking: " + e.getMessage());
        }
        
        return REDIRECT_BOOKINGS;
    }
    
    // Xác nhận booking + tất cả dịch vụ
    @PostMapping("/{id}/confirm-all")
    public String confirmBookingAndAllServices(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_BOOKINGS;
            }
            
            // Xác nhận booking
            booking.setStatus(STATUS_CONFIRMED);
            
            // Cập nhật trạng thái phòng
            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(false);
                room.setStatus("BOOKED");
                roomService.saveRoom(room);
            }
            
            bookingService.save(booking);
            
            // Xác nhận tất cả dịch vụ liên quan
            List<ServiceOrder> services = serviceOrderService.findByBookingId(id);
            int confirmedServices = 0;
            
            for (ServiceOrder service : services) {
                if (BOOKING_STATUS_PENDING.equals(service.getStatus())) {
                    service.setStatus(STATUS_PROCESSING);
                    serviceOrderService.save(service);
                    confirmedServices++;
                }
            }
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                "Đã xác nhận booking #" + id + " và " + confirmedServices + " dịch vụ cho khách hàng " + booking.getCustomerName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi xác nhận booking + dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_BOOKINGS;
    }

    // Từ chối booking
    @PostMapping("/{id}/reject")
    public String rejectBooking(@PathVariable Long id, @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_BOOKINGS;
            }
            
            booking.setStatus(BOOKING_STATUS_REJECTED + (reason != null && !reason.trim().isEmpty() ? " - " + reason : ""));
            
            // Trả lại trạng thái phòng
            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(true);
                room.setStatus("AVAILABLE");
                roomService.saveRoom(room);
            }
            
            bookingService.save(booking);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                "Đã từ chối booking #" + id + " cho khách hàng " + booking.getCustomerName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi từ chối booking: " + e.getMessage());
        }
        
        return REDIRECT_BOOKINGS;
    }
    
    // Xóa booking
    @PostMapping("/{id}/delete")
    public String deleteBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.findById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_BOOKINGS;
            }
            
            // Xóa tất cả dịch vụ liên quan trước
            List<ServiceOrder> services = serviceOrderService.findByBookingId(id);
            for (ServiceOrder service : services) {
                serviceOrderService.deleteById(service.getId());
            }
            
            // Trả lại trạng thái phòng
            Room room = booking.getRoom();
            if (room != null) {
                room.setAvailable(true);
                room.setStatus("AVAILABLE");
                roomService.saveRoom(room);
            }
            
            // Xóa booking
            bookingService.deleteById(id);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                "Đã xóa booking #" + id + " và " + services.size() + " dịch vụ liên quan");
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi xóa booking: " + e.getMessage());
        }
        
        return REDIRECT_BOOKINGS;
    }
}
