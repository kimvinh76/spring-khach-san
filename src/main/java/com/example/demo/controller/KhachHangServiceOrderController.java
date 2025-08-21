package com.example.demo.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/services")
public class KhachHangServiceOrderController {

    private static final Logger logger = LoggerFactory.getLogger(KhachHangServiceOrderController.class);

    private static final String CURRENT_USER_ATTR = "currentUser";
    private static final String FLASH_ERROR = "error";
    private static final String FLASH_SUCCESS = "success";
    private static final String REDIRECT_LOGIN = "redirect:/auth/login";
    private static final String REDIRECT_SERVICES_LIST = "redirect:/khachhang/services/list";
    private static final String REDIRECT_MY_ORDERS_PREFIX = "redirect:/khachhang/services/my-orders?bookingId=";

    private final ServiceOrderService serviceOrderService;
    private final BookingService bookingService;

    public KhachHangServiceOrderController(ServiceOrderService serviceOrderService, BookingService bookingService) {
        this.serviceOrderService = serviceOrderService;
        this.bookingService = bookingService;
    }

    @PostMapping("/order")
    public String createServiceOrder(
            @RequestParam("serviceName") String serviceName,
            @RequestParam("servicePrice") Long servicePrice,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("bookingId") Long bookingId,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = (User) session.getAttribute(CURRENT_USER_ATTR);
            if (user == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Vui lòng đăng nhập để đặt dịch vụ.");
                return REDIRECT_LOGIN;
            }

            if (bookingId == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Thiếu thông tin booking.");
                return REDIRECT_SERVICES_LIST;
            }

            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Không tìm thấy booking hợp lệ.");
                return REDIRECT_SERVICES_LIST;
            }
            // Ownership check (userId or customerName fallback)
            boolean isOwner = (booking.getUserId() != null && booking.getUserId().equals(user.getId()))
                           || (booking.getCustomerName() != null && booking.getCustomerName().equalsIgnoreCase(user.getUsername()));
            if (!isOwner) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Bạn không có quyền đặt dịch vụ cho booking này.");
                return REDIRECT_SERVICES_LIST;
            }

            if (quantity == null || quantity < 1) quantity = 1;
            double total = (servicePrice != null ? servicePrice.doubleValue() : 0d) * quantity;

            ServiceOrder order = new ServiceOrder();
            order.setServiceName(serviceName);
            order.setDescription(serviceName);
            order.setPrice(servicePrice != null ? servicePrice.doubleValue() : 0d);
            order.setQuantity(quantity);
            order.setTotalAmount(total);
            order.setOrderTime(LocalDateTime.now());
            order.setCustomerName(booking.getCustomerName());
            order.setPhone(booking.getPhone());
            order.setBookingId(booking.getId());
            order.setRoomName(booking.getRoom() != null ? booking.getRoom().getName() : null);
            order.setUserId(user.getId());
            order.setNote(note);
            order.setStatus("Chờ xác nhận");

            serviceOrderService.save(order);

            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "Đã gửi yêu cầu đặt dịch vụ: " + serviceName + ". Vui lòng đợi admin xác nhận.");
            return REDIRECT_SERVICES_LIST + "?bookingId=" + bookingId;
        } catch (Exception e) {
            logger.error("Lỗi khi đặt dịch vụ", e);
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi đặt dịch vụ: " + e.getMessage());
            return REDIRECT_SERVICES_LIST + "?bookingId=" + bookingId;
        }
    }

    // Khách hủy yêu cầu dịch vụ khi còn trong trạng thái cho phép
    @PostMapping("/cancel/{id}")
    public String cancelServiceOrder(@PathVariable("id") Long id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute(CURRENT_USER_ATTR);
            if (user == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Vui lòng đăng nhập để hủy dịch vụ.");
                return REDIRECT_LOGIN;
            }

            ServiceOrder order = serviceOrderService.findById(id).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Không tìm thấy đơn dịch vụ.");
                return "redirect:/khachhang/services/my-orders";
            }

            // Chỉ chủ sở hữu booking/user mới được hủy
            boolean isOwner = (order.getUserId() != null && order.getUserId().equals(user.getId()))
                           || (order.getCustomerName() != null && order.getCustomerName().equalsIgnoreCase(user.getUsername()));
            if (!isOwner) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Bạn không có quyền hủy đơn dịch vụ này.");
                return REDIRECT_MY_ORDERS_PREFIX + order.getBookingId();
            }

            // Chỉ cho hủy khi chưa hoàn thành và chưa tạo hóa đơn
            String st = order.getStatus() != null ? order.getStatus() : "";
            boolean cancellable = (st.contains("Chờ xác nhận") || st.contains("Đang xử lý")) && order.getInvoice() == null;
            if (!cancellable) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, "Đơn dịch vụ không thể hủy (đã hoàn thành hoặc đã xuất hóa đơn).");
                return REDIRECT_MY_ORDERS_PREFIX + order.getBookingId();
            }

            order.setStatus("Từ chối - Khách hủy");
            serviceOrderService.save(order);

            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "Đã hủy đơn dịch vụ thành công.");
            return REDIRECT_MY_ORDERS_PREFIX + order.getBookingId();
        } catch (Exception e) {
            logger.error("Có lỗi khi hủy dịch vụ", e);
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Có lỗi khi hủy dịch vụ: " + e.getMessage());
            return "redirect:/khachhang/services/my-orders";
        }
    }

    // Customer: view service order history - EXPLICIT MAPPING TO AVOID CONFLICTS
    @GetMapping(value = "/history", name = "serviceHistory")
    public String serviceOrderHistory(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute(CURRENT_USER_ATTR);
        if (user == null) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Vui lòng đăng nhập để xem lịch sử dịch vụ.");
            return REDIRECT_LOGIN;
        }
        // Load orders for this user
        java.util.List<ServiceOrder> orders = serviceOrderService.getServiceOrdersByUserId(user.getId());
        model.addAttribute("orders", orders != null ? orders : new java.util.ArrayList<>());
        model.addAttribute("user", user);
        return "khachhang/services/history";
    }

    // Block any /{id} pattern that's not a number - this should catch the issue
    @GetMapping("/{pathVar}")
    public String handleInvalidId(@PathVariable String pathVar, RedirectAttributes redirectAttributes) {
        // If someone hits /services/history or /services/anything-non-numeric, redirect appropriately
        if ("history".equals(pathVar)) {
            return "redirect:/services/history";
        }
        redirectAttributes.addFlashAttribute(FLASH_ERROR, "Đường dẫn không hợp lệ: " + pathVar);
        return REDIRECT_SERVICES_LIST;
    }
}
