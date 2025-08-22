package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Booking;
import com.example.demo.model.Room;
import com.example.demo.service.BookingService;
import com.example.demo.service.RoomService;

/**
 * Controller cho khách đặt phòng
 */
@Controller
@RequestMapping("/booking")
public class BookingController {
    private final BookingService bookingService;
    private final RoomService roomService;
    private final com.example.demo.service.RoomTypeService roomTypeService;
    private final com.example.demo.service.ServiceOrderService serviceOrderService;

    public BookingController(BookingService bookingService, RoomService roomService, com.example.demo.service.RoomTypeService roomTypeService, com.example.demo.service.ServiceOrderService serviceOrderService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
        this.serviceOrderService = serviceOrderService;
    }

    // Hiển thị form đặt phòng cho khách, lọc theo loại phòng và phân trang
    @GetMapping("/new")
    public String showBookingForm(@RequestParam(value = "roomId", required = false) Long roomId, Model model) {
        if (roomId == null) {
            model.addAttribute("error", "Vui lòng chọn phòng để đặt.");
            return "redirect:/rooms";
        }
        
        Room room = roomService.getRoomById(roomId).orElse(null);
        if (room == null) {
            model.addAttribute("error", "Không tìm thấy phòng với ID: " + roomId);
            return "redirect:/rooms";
        }
        if (!room.getAvailable()) {
            model.addAttribute("error", "Phòng này hiện không còn trống.");
            return "redirect:/rooms";
        }
        Booking booking = new Booking();
        booking.setRoom(room);
        model.addAttribute("room", room);
        model.addAttribute("booking", booking);
        return "khachhang/booking/form";
    }

    // Xử lý đặt phòng với kiểm tra trạng thái real-time
    @PostMapping
    public String createBooking(@ModelAttribute Booking booking, @RequestParam("room.id") Long roomId, 
                               Model model, RedirectAttributes redirectAttributes, jakarta.servlet.http.HttpSession session) {
        
        System.out.println("=== DEBUG BOOKING START ===");
        System.out.println("Room ID: " + roomId);
        System.out.println("Customer Name: " + booking.getCustomerName());
        System.out.println("Phone: " + booking.getPhone());
        System.out.println("Check-in: " + booking.getCheckIn());
        System.out.println("Check-out: " + booking.getCheckOut());
        
        try {
            // Gán userId cho booking nếu đã đăng nhập
            com.example.demo.model.User currentUser = (com.example.demo.model.User) session.getAttribute("currentUser");
            if (currentUser != null) {
                booking.setUserId(currentUser.getId());
                booking.setCustomerName(currentUser.getFullName());
                booking.setPhone(currentUser.getPhone());
                booking.setEmail(currentUser.getEmail());
            }
            // Kiểm tra phòng tồn tại
            Room room = roomService.getRoomById(roomId).orElse(null);
            if (room == null) {
                System.out.println("ERROR: Room not found!");
                model.addAttribute("error", "Phòng không tồn tại!");
                return "khachhang/booking/form";
            }
            
            System.out.println("Room found: " + room.getName() + ", Available: " + room.getAvailable());
            
            // Kiểm tra trạng thái phòng
            if (!room.getAvailable()) {
                System.out.println("ERROR: Room not available!");
                model.addAttribute("error", "Phòng " + room.getName() + " hiện đã được đặt. Vui lòng chọn phòng khác!");
                model.addAttribute("room", room);
                model.addAttribute("booking", booking);
                return "khachhang/booking/form";
            }
            
            // Validate ngày
            if (booking.getCheckIn() == null || booking.getCheckOut() == null) {
                model.addAttribute("error", "Vui lòng chọn ngày nhận và trả phòng!");
                model.addAttribute("room", room);
                model.addAttribute("booking", booking);
                return "khachhang/booking/form";
            }
            
            if (!booking.getCheckOut().isAfter(booking.getCheckIn())) {
                model.addAttribute("error", "Ngày trả phòng phải sau ngày nhận phòng!");
                model.addAttribute("room", room);
                model.addAttribute("booking", booking);
                return "khachhang/booking/form";
            }
            
            // Tạm thời giữ chỗ phòng (chưa chính thức đặt)
            // Phòng sẽ được set thành OCCUPIED khi admin xác nhận
            // room.setAvailable(false); // Không set ngay, chờ admin xác nhận
            // room.setStatus("OCCUPIED"); // Không set ngay
            // roomService.saveRoom(room);
            System.out.println("Room will be reserved after admin confirmation");
            
            // Tạo booking với trạng thái chờ xác nhận
            booking.setRoom(room);
            booking.setStatus("Chờ xác nhận"); // Admin cần xác nhận
            booking.setBookingDate(java.time.LocalDateTime.now());
            Booking saved = bookingService.saveBooking(booking);
            System.out.println("Booking saved with ID: " + saved.getId());
            
            System.out.println("=== DEBUG BOOKING SUCCESS ===");
            
            // Chuyển hướng trực tiếp đến trang hóa đơn
            return "redirect:/booking/invoice/" + saved.getId();
            
        } catch (Exception e) {
            System.out.println("ERROR saving booking: " + e.getMessage());
            e.printStackTrace();
            // In ra lỗi chi tiết để debug
            String errorMsg = "Có lỗi xảy ra khi đặt phòng: " + e.getMessage();
            System.out.println("[ERROR] " + errorMsg);
            model.addAttribute("error", errorMsg);
            
            // Lấy lại thông tin phòng để hiển thị form
            Room room = roomService.getRoomById(roomId).orElse(null);
            model.addAttribute("room", room);
            model.addAttribute("booking", booking);
            return "khachhang/booking/form";
        }
    }

    // Trang hóa đơn chi tiết sau khi đặt phòng
    @GetMapping("/invoice/{id}")
    public String bookingInvoice(@PathVariable Long id, Model model) {
    Booking booking = bookingService.getBookingById(id).orElse(null);
    if (booking == null) return "redirect:/rooms";
    long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
    if (nights < 1) nights = 1;
    Double priceBase = booking.getRoom().getRoomType().getPriceBase();
    long roomTotal = nights * (priceBase != null ? priceBase.longValue() : 0);

    // Lấy tất cả dịch vụ theo booking
    List<com.example.demo.model.ServiceOrder> serviceOrders = serviceOrderService.findByBookingId(booking.getId());

    // Dịch vụ đã xác nhận/hoàn thành (đủ điều kiện tính tiền)
    double confirmedServiceTotal = 0.0;
    int confirmedServices = 0;
    int pendingServices = 0;
    if (serviceOrders != null && !serviceOrders.isEmpty()) {
        for (com.example.demo.model.ServiceOrder o : serviceOrders) {
            if (o.getTotalAmount() != null) {
                String st = o.getStatus() != null ? o.getStatus() : "";
                if ("Chờ xác nhận".equals(st)) {
                    pendingServices++;
                } else if (!"Từ chối".equals(st)) {
                    confirmedServiceTotal += o.getTotalAmount();
                    confirmedServices++;
                }
            }
        }
    }

    // Tính phần dịch vụ chưa xuất hóa đơn để hiển thị nút thanh toán phát sinh
    java.util.List<com.example.demo.model.ServiceOrder> payableServices = serviceOrderService.findUnpaidConfirmedByBookingId(booking.getId());
    double servicePayableTotal = payableServices.stream()
        .filter(o -> o.getTotalAmount() != null)
        .mapToDouble(com.example.demo.model.ServiceOrder::getTotalAmount)
        .sum();

    boolean roomAlreadyPaid = booking.getStatus() != null && booking.getStatus().contains("Đã thanh toán");
    long total = roomTotal + (long) confirmedServiceTotal; // full total (room + confirmed services)
    
    // Stage 1: Room + confirmed services at booking time (pay together)
    // Stage 2: Additional services confirmed after room payment
    long servicePaidPortion = (long) (confirmedServiceTotal - servicePayableTotal);
    if (servicePaidPortion < 0) servicePaidPortion = 0;
    
    long displayTotal; // amount to show for current payment stage
    if (!roomAlreadyPaid) {
        // Stage 1: Pay room + existing confirmed services together
        displayTotal = roomTotal + (long) confirmedServiceTotal;
    } else {
        // Stage 2: Only show additional unpaid services
        displayTotal = roomTotal + servicePaidPortion; // what's already paid
    }
    model.addAttribute("booking", booking);
    model.addAttribute("nights", nights);
    model.addAttribute("roomTotal", roomTotal);
    model.addAttribute("serviceTotal", (long) confirmedServiceTotal);
    model.addAttribute("total", total); // keeps legacy usage
    model.addAttribute("displayTotal", displayTotal);
    model.addAttribute("fullTotal", total);
    model.addAttribute("servicePaidTotal", servicePaidPortion);
    model.addAttribute("roomPayable", roomPayable);
    model.addAttribute("serviceOrders", serviceOrders);
    model.addAttribute("canOrderServices", true);
    model.addAttribute("roomAlreadyPaid", roomAlreadyPaid);
    model.addAttribute("servicePayableTotal", (long) servicePayableTotal);
    model.addAttribute("payableServicesCount", payableServices.size());
    model.addAttribute("pendingServices", pendingServices);
    model.addAttribute("confirmedServices", confirmedServices);
    return "khachhang/booking/invoice";
    }

    // Trang thanh toán
    @GetMapping("/payment/{id}")
    public String showPaymentForm(@PathVariable Long id, Model model) {
    // Legacy endpoint: redirect to new unified staged payment controller
    return "redirect:/khachhang/payment/" + id;
    }

    // Xử lý thanh toán
    @PostMapping("/payment/{id}")
    public String processPayment(@PathVariable Long id, @RequestParam String paymentMethod, Model model) {
    // Legacy endpoint form submission: forward to new payment page
    return "redirect:/khachhang/payment/" + id;
    }

    // Trang xác nhận đặt phòng thành công
    @GetMapping("/success")
    public String bookingSuccess() {
        return "khachhang/booking/success";
    }

    // Xem lịch sử đặt phòng của khách (theo tên hoặc sđt)
    @GetMapping("/history")
    public String bookingHistory(jakarta.servlet.http.HttpSession session, Model model) {
        com.example.demo.model.User currentUser = (com.example.demo.model.User) session.getAttribute("currentUser");
        if (currentUser == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để xem lịch sử đặt phòng!");
            model.addAttribute("bookings", new ArrayList<>());
            return "khachhang/booking/history";
        }
        List<Booking> bookings = bookingService.getBookingsByUserId(currentUser.getId());
        model.addAttribute("bookings", bookings);
        return "khachhang/booking/history";
    }

    // Xem trạng thái booking (dành cho khách hàng)
    @GetMapping("/status/{id}")
    public String bookingStatus(@PathVariable Long id, jakarta.servlet.http.HttpSession session, Model model) {
        com.example.demo.model.User currentUser = (com.example.demo.model.User) session.getAttribute("currentUser");
        if (currentUser == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để xem trạng thái booking!");
            return "redirect:/auth/login";
        }
        
        Booking booking = bookingService.getBookingById(id).orElse(null);
        if (booking == null) {
            model.addAttribute("error", "Không tìm thấy booking!");
            return "redirect:/booking/history";
        }
        
        // Kiểm tra quyền: chỉ cho phép chủ booking xem
        if (!booking.getUserId().equals(currentUser.getId())) {
            model.addAttribute("error", "Bạn không có quyền xem booking này!");
            return "redirect:/booking/history";
        }
        
        // Lấy danh sách dịch vụ liên quan
        List<com.example.demo.model.ServiceOrder> relatedServices = serviceOrderService.findByBookingId(id);
        
        model.addAttribute("booking", booking);
        model.addAttribute("relatedServices", relatedServices);
        
        return "khachhang/booking/status";
    }

    // Xem chi tiết booking
    @GetMapping("/detail/{id}")
    public String bookingDetail(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id).orElse(null);
        model.addAttribute("booking", booking);
        return "khachhang/booking/detail";
    }

    // Cho phép khách chọn dịch vụ khi đặt phòng (giả sử có danh sách dịch vụ)
    @GetMapping("/service-select")
    public String selectService(Model model) {
        // TODO: Lấy danh sách dịch vụ từ ServiceOrderService hoặc ServiceRepository
        // model.addAttribute("services", serviceList);
        return "khachhang/booking/service_select";
    }
}
