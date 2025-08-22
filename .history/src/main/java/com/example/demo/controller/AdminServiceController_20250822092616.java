package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.AggregatedOrder;
import com.example.demo.model.ServiceOrder;
import com.example.demo.service.ServiceOrderService;

@Controller
@RequestMapping("/admin/services")
public class AdminServiceController {
    
    private final ServiceOrderService serviceOrderService;

    public AdminServiceController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    private static final String STATUS_PROCESSING = "Đang xử lý";
    private static final String STATUS_COMPLETED = "Hoàn thành";
    private static final String STATUS_REJECTED = "Từ chối";
    private static final String STATUS_PENDING = "Chờ xác nhận";
    private static final String REDIRECT_SERVICES = "redirect:/admin/services";
    private static final String FLASH_SUCCESS = "success";
    private static final String FLASH_ERROR = "error";
    private static final String MSG_NOT_FOUND = "Không tìm thấy đơn dịch vụ!";
    private static final String MSG_CONFIRM_SUCCESS = "Đã xác nhận đơn dịch vụ #";
    private static final String MSG_CUSTOMER = " cho khách hàng ";

    // Quản lý tất cả dịch vụ với khả năng xác nhận
    @GetMapping
    public String listServices(@RequestParam(defaultValue = "") String status, Model model) {
        List<ServiceOrder> services;
        if (!status.isEmpty()) {
            services = serviceOrderService.findByStatusContaining(status);
        } else {
            services = serviceOrderService.findAll();
        }
        
        // Gom nhóm dịch vụ theo booking để dễ quản lý
        Map<Long, List<ServiceOrder>> servicesByBooking = services.stream()
            .filter(s -> s.getBookingId() != null)
            .collect(java.util.stream.Collectors.groupingBy(ServiceOrder::getBookingId));

        // Build aggregated view per booking (group by serviceType inside each booking)
        java.util.Map<Long, List<AggregatedOrder>> servicesAggregatedByBooking = new java.util.LinkedHashMap<>();
        for (Map.Entry<Long, List<ServiceOrder>> e : servicesByBooking.entrySet()) {
            java.util.Map<String, AggregatedOrder> m = new java.util.LinkedHashMap<>();
            for (ServiceOrder so : e.getValue()) {
                String key = so.getServiceType() != null ? so.getServiceType() : "Unknown";
                AggregatedOrder ag = m.computeIfAbsent(key, k -> new AggregatedOrder(k));
                ag.addOrder(so);
            }
            servicesAggregatedByBooking.put(e.getKey(), new java.util.ArrayList<>(m.values()));
        }
        
        model.addAttribute("services", services);
        model.addAttribute("servicesByBooking", servicesByBooking);
    model.addAttribute("servicesAggregatedByBooking", servicesAggregatedByBooking);
        model.addAttribute("selectedStatus", status);
        
        // Thống kê
        long pendingCount = services.stream().filter(s -> STATUS_PENDING.equals(s.getStatus())).count();
        long processingCount = services.stream().filter(s -> STATUS_PROCESSING.equals(s.getStatus())).count();
        long completedCount = services.stream().filter(s -> STATUS_COMPLETED.equals(s.getStatus())).count();
        long rejectedCount = services.stream().filter(s -> s.getStatus() != null && s.getStatus().contains("Từ chối")).count();
        
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        
        // Tính tổng doanh thu
        double totalRevenue = services.stream()
            .filter(service -> service.getTotalAmount() != null)
            .filter(service -> !"Từ chối".equals(service.getStatus()))
            .mapToDouble(ServiceOrder::getTotalAmount)
            .sum();
        model.addAttribute("totalRevenue", totalRevenue);
        
        return "admin/services/list";
    }
    
    // Xác nhận tất cả dịch vụ của một booking
    @PostMapping("/confirm-booking/{bookingId}")
    public String confirmAllServicesByBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            List<ServiceOrder> services = serviceOrderService.findByBookingId(bookingId);
            int confirmedCount = 0;
            
            for (ServiceOrder service : services) {
                if (STATUS_PENDING.equals(service.getStatus())) {
                    // Chuyển trực tiếp sang "Hoàn thành" thay vì "Đang xử lý"
                    service.setStatus(STATUS_COMPLETED);
                    serviceOrderService.save(service);
                    confirmedCount++;
                }
            }
            
            if (confirmedCount > 0) {
                redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                    "Đã hoàn thành " + confirmedCount + " dịch vụ cho booking #" + bookingId);
            } else {
                redirectAttributes.addFlashAttribute("info", 
                    "Không có dịch vụ nào cần xác nhận cho booking #" + bookingId);
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi xác nhận dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_SERVICES;
    }

    // Hoàn thành tất cả dịch vụ đang xử lý của một booking
    @PostMapping("/complete-booking/{bookingId}")
    public String completeAllServicesByBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            List<ServiceOrder> services = serviceOrderService.findByBookingId(bookingId);
            int completedCount = 0;
            
            for (ServiceOrder service : services) {
                if (STATUS_PROCESSING.equals(service.getStatus())) {
                    service.setStatus(STATUS_COMPLETED);
                    serviceOrderService.save(service);
                    completedCount++;
                }
            }
            
            if (completedCount > 0) {
                redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                    "Đã hoàn thành " + completedCount + " dịch vụ cho booking #" + bookingId);
            } else {
                redirectAttributes.addFlashAttribute("info", 
                    "Không có dịch vụ nào đang xử lý cho booking #" + bookingId);
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi hoàn thành dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_SERVICES;
    }

    // Xác nhận đơn dịch vụ
    @PostMapping("/{id}/confirm")
    public String confirmServiceOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ServiceOrder serviceOrder = serviceOrderService.findById(id).orElse(null);
            if (serviceOrder == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_SERVICES;
            }
            
            serviceOrder.setStatus(STATUS_PROCESSING);
            serviceOrderService.save(serviceOrder);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                MSG_CONFIRM_SUCCESS + id + MSG_CUSTOMER + serviceOrder.getCustomerName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi xác nhận đơn dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_SERVICES;
    }
    
    // Từ chối đơn dịch vụ
    @PostMapping("/{id}/reject")
    public String rejectServiceOrder(@PathVariable Long id, @RequestParam(required = false) String reason,
                                   RedirectAttributes redirectAttributes) {
        try {
            ServiceOrder serviceOrder = serviceOrderService.findById(id).orElse(null);
            if (serviceOrder == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_SERVICES;
            }
            
            serviceOrder.setStatus(STATUS_REJECTED + (reason != null && !reason.trim().isEmpty() ? " - " + reason : ""));
            serviceOrderService.save(serviceOrder);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                "Đã từ chối đơn dịch vụ #" + id + " cho khách hàng " + serviceOrder.getCustomerName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi từ chối đơn dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_SERVICES;
    }
    
    // Hoàn thành đơn dịch vụ  
    @PostMapping("/{id}/complete")
    public String completeServiceOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ServiceOrder serviceOrder = serviceOrderService.findById(id).orElse(null);
            if (serviceOrder == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_SERVICES;
            }
            
            serviceOrder.setStatus(STATUS_COMPLETED);
            ServiceOrder saved = serviceOrderService.save(serviceOrder);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, 
                "Đã hoàn thành đơn dịch vụ #" + id + " cho khách hàng " + serviceOrder.getCustomerName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi hoàn thành đơn dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_SERVICES;
    }

    // Lịch sử tất cả dịch vụ - CHỈ dành cho admin
    @GetMapping("/history")
    public String serviceHistory(@RequestParam(required = false) String customerName, 
                                @RequestParam(required = false) String phone,
                                Model model) {
        List<ServiceOrder> services = new java.util.ArrayList<>();
        
        if (customerName != null && !customerName.trim().isEmpty()) {
            services = serviceOrderService.findByCustomerName(customerName);
        } else if (phone != null && !phone.trim().isEmpty()) {
            services = serviceOrderService.findByPhone(phone);
        } else {
            // Admin có thể xem tất cả dịch vụ nếu không tìm kiếm
            services = serviceOrderService.findAll();
        }
        
        model.addAttribute("services", services);
        model.addAttribute("customerName", customerName);
        model.addAttribute("phone", phone);
        
        // Tính tổng doanh thu
        double totalRevenue = services.stream()
            .filter(service -> service.getTotalAmount() != null)
            .mapToDouble(ServiceOrder::getTotalAmount)
            .sum();
        model.addAttribute("totalRevenue", totalRevenue);
        
        // Thống kê theo trạng thái
        long completedCount = services.stream()
            .filter(service -> "Hoàn thành".equals(service.getStatus()))
            .count();
        long processingCount = services.stream()
            .filter(service -> "Đang xử lý".equals(service.getStatus()))
            .count();
            
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("totalCount", services.size());
        
        return "admin/services/history";
    }
    
    // Xóa dịch vụ
    @PostMapping("/{id}/delete")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ServiceOrder serviceOrder = serviceOrderService.findById(id).orElse(null);
            if (serviceOrder == null) {
                redirectAttributes.addFlashAttribute(FLASH_ERROR, MSG_NOT_FOUND);
                return REDIRECT_SERVICES;
            }
            
            String serviceInfo = serviceOrder.getServiceType() + " - " + serviceOrder.getDescription();
            serviceOrderService.deleteById(id);
            
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "Đã xóa dịch vụ: " + serviceInfo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Lỗi khi xóa dịch vụ: " + e.getMessage());
        }
        
        return REDIRECT_SERVICES;
    }
}
