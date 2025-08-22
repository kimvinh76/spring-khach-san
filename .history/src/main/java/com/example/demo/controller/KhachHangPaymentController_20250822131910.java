package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Booking;
import com.example.demo.model.Invoice;
import com.example.demo.model.ServiceOrder;
import com.example.demo.model.User;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.ServiceOrderService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/khachhang/payment")
public class KhachHangPaymentController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private ServiceOrderService serviceOrderService;

    @Autowired
    private InvoiceRepository invoiceRepository;
    
    

    // Trang thanh toán cho khách hàng
    @GetMapping("/{id}")
    public String showCustomerPaymentForm(@PathVariable Long id, Model model, 
                                        HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra đăng nhập
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán");
                return "redirect:/auth/login";
            }

            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy booking");
                return "redirect:/khachhang/services/my-orders";
            }

            // Kiểm tra quyền sở hữu booking
            boolean isOwner = false;
            if (booking.getUserId() != null && booking.getUserId().equals(user.getId())) {
                isOwner = true;
            } else if (booking.getCustomerName() != null && booking.getCustomerName().equalsIgnoreCase(user.getUsername())) {
                isOwner = true;
            }

            if (!isOwner) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thanh toán booking này");
                return "redirect:/khachhang/services/my-orders";
            }

            // Logic linh hoạt cho thanh toán
            boolean canPay = false;
            String paymentMessage = "";
            
            if (booking.getStatus() == null) {
                paymentMessage = "Booking chưa có trạng thái. Vui lòng liên hệ admin.";
            } else if (booking.getStatus().contains("Đã thanh toán")) {
                paymentMessage = "Booking này đã được thanh toán thành công!";
            } else if (booking.getStatus().equals("Đã xác nhận")) {
                canPay = true;
                paymentMessage = "Booking đã được xác nhận. Bạn có thể thanh toán ngay!";
            } else if (booking.getStatus().equals("Chờ xác nhận")) {
                paymentMessage = "Booking đang chờ admin xác nhận. Bạn có thể thanh toán trước để ưu tiên xử lý.";
                canPay = true; // Cho phép thanh toán trước
            } else if (booking.getStatus().contains("từ chối")) {
                paymentMessage = "Booking đã bị từ chối. Không thể thanh toán.";
            } else {
                paymentMessage = "Trạng thái booking: " + booking.getStatus();
                canPay = true; // Mặc định cho phép thanh toán
            }

            // Tính toán số tiền
            long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
            if (nights < 1) nights = 1;
            
            Double priceBase = booking.getRoom().getRoomType().getPriceBase();
            long roomTotal = nights * (priceBase != null ? priceBase.longValue() : 0);
            
            // Lấy tất cả dịch vụ (không phân biệt trạng thái)
            List<ServiceOrder> serviceOrders = serviceOrderService.findByBookingId(booking.getId());
            double serviceTotal = 0.0; // all services excluding explicit rejected
            int confirmedServices = 0;
            int pendingServices = 0;
            
            if (serviceOrders != null && !serviceOrders.isEmpty()) {
                for (ServiceOrder order : serviceOrders) {
                    if (order.getTotalAmount() != null) {
                        if ("Chờ xác nhận".equals(order.getStatus())) {
                            pendingServices++;
                        } else if (!"Từ chối".equals(order.getStatus())) {
                            serviceTotal += order.getTotalAmount();
                            confirmedServices++;
                        }
                    }
                }
            }

            // Calculate delta payable parts
            boolean roomAlreadyPaid = booking.getStatus() != null && booking.getStatus().contains("Đã thanh toán");
            
            // Stage 1: Room + confirmed services at time of booking (pay together)
            // Stage 2: Additional services confirmed after room payment
            long roomAndInitialServicesTotal = roomTotal + (long) serviceTotal;
            
            // Eligible services (confirmed/completed & unpaid)
            java.util.List<ServiceOrder> payableServices = serviceOrderService.findUnpaidConfirmedByBookingId(booking.getId());
            double servicePayableTotal = payableServices.stream()
                .filter(o -> o.getTotalAmount() != null)
                .mapToDouble(ServiceOrder::getTotalAmount)
                .sum();

            // Paid portion of services (confirmed but already invoiced) = all confirmed - unpaid confirmed
            double servicePaidPortion = serviceTotal - servicePayableTotal;
            if (servicePaidPortion < 0) servicePaidPortion = 0; // guard

            // Full final total (room + all confirmed services) for reference
            long fullTotal = roomTotal + (long) serviceTotal;

            // Display total logic:
            //  - If room not yet paid: show room + existing confirmed services (stage 1)
            //  - If room paid: show only additional unpaid services (stage 2)
            long displayTotal;
            if (!roomAlreadyPaid) {
                displayTotal = roomAndInitialServicesTotal; // stage 1: room + confirmed services
            } else {
                displayTotal = (long) servicePayableTotal; // stage 2: only additional services
            }

            // Thông báo về dịch vụ
            String serviceMessage = "";
            if (pendingServices > 0) {
                serviceMessage = "Bạn có " + pendingServices + " dịch vụ đang chờ xác nhận. " +
                               "Số tiền này sẽ được cập nhật sau khi admin xác nhận.";
            }
            // Re-evaluate canPay based on payment stages
            if (!roomAlreadyPaid) {
                canPay = true; // stage 1: room + confirmed services
                paymentMessage = "Thanh toán tiền phòng và dịch vụ đã xác nhận.";
                System.out.println("DEBUG: Stage 1 - Room not paid yet, canPay=true, displayTotal=" + displayTotal);
            } else if (servicePayableTotal > 0) {
                canPay = true; // stage 2: additional services only
                paymentMessage = "Bạn có dịch vụ phát sinh chưa thanh toán.";
                System.out.println("DEBUG: Stage 2 - Room paid, services pending, canPay=true, servicePayableTotal=" + servicePayableTotal);
            } else {
                canPay = false;
                paymentMessage = "Booking đã thanh toán đầy đủ.";
                System.out.println("DEBUG: All paid - canPay=false");
            }
            
            // Force enable payment for testing (comment out in production)
            if (!canPay && displayTotal > 0) {
                canPay = true;
                paymentMessage = "Sẵn sàng thanh toán.";
                System.out.println("DEBUG: Force enabled payment, displayTotal=" + displayTotal);
            }
            
            // Calculate roomPayable for template compatibility
            long roomPayable = roomAlreadyPaid ? 0 : roomTotal;

            model.addAttribute("booking", booking);
            model.addAttribute("nights", nights);
            model.addAttribute("roomTotal", roomTotal);
            model.addAttribute("serviceTotal", (long) serviceTotal);
            model.addAttribute("total", displayTotal); // total to display (dynamic)
            model.addAttribute("fullTotal", fullTotal); // final total including unpaid confirmed services
            model.addAttribute("servicePaidTotal", (long) servicePaidPortion);
            model.addAttribute("displayTotal", displayTotal);
            model.addAttribute("servicePayableTotal", (long) servicePayableTotal);
            model.addAttribute("roomPayable", roomPayable);
            model.addAttribute("roomAlreadyPaid", roomAlreadyPaid);
            model.addAttribute("roomAndInitialServicesTotal", roomAndInitialServicesTotal);
            model.addAttribute("serviceOrders", serviceOrders);
            model.addAttribute("canPay", canPay);
            model.addAttribute("paymentMessage", paymentMessage);
            model.addAttribute("serviceMessage", serviceMessage);
            model.addAttribute("confirmedServices", confirmedServices);
            model.addAttribute("pendingServices", pendingServices);
            model.addAttribute("user", user);
            model.addAttribute("payableServicesCount", payableServices.size());
            model.addAttribute("serviceOnly", roomAlreadyPaid && servicePayableTotal > 0);

            return "khachhang/payment/form";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/khachhang/services/my-orders";
        }
    }

    // Xử lý thanh toán cho khách hàng
    @PostMapping("/{id}")
    public String processCustomerPayment(@PathVariable Long id, HttpSession session, 
                                       RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra đăng nhập
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán");
                return "redirect:/auth/login";
            }

            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy booking");
                return "redirect:/khachhang/services/my-orders";
            }

            // Kiểm tra quyền sở hữu
            boolean isOwner = false;
            if (booking.getUserId() != null && booking.getUserId().equals(user.getId())) {
                isOwner = true;
            } else if (booking.getCustomerName() != null && booking.getCustomerName().equalsIgnoreCase(user.getUsername())) {
                isOwner = true;
            }

            if (!isOwner) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thanh toán booking này");
                return "redirect:/khachhang/services/my-orders";
            }

            // Không cho thanh toán lại tiền phòng nếu đã thanh toán
            if (booking.getStatus() != null && booking.getStatus().contains("Đã thanh toán")) {
                redirectAttributes.addFlashAttribute("info", "Phòng đã thanh toán. Nếu có dịch vụ phát sinh, hãy dùng nút 'Thanh toán dịch vụ'.");
                return "redirect:/booking/invoice/" + id;
            }

            // Tính tổng tiền cần thanh toán (phòng + dịch vụ đã xác nhận)
            Long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
            Double priceBase = booking.getRoom().getRoomType().getPriceBase();
            long roomTotal = nights * (priceBase != null ? priceBase.longValue() : 0);
            
            // Lấy dịch vụ đã xác nhận
            java.util.List<ServiceOrder> confirmedServices = serviceOrderService.findConfirmedByBookingId(id);
            double serviceTotal = confirmedServices.stream()
                .filter(o -> o.getTotalAmount() != null)
                .mapToDouble(ServiceOrder::getTotalAmount)
                .sum();
                
            double totalAmount = roomTotal + serviceTotal;

            // Tạo hóa đơn cho thanh toán phòng + dịch vụ giai đoạn 1
            Invoice invoice = new Invoice();
            invoice.setCustomerName(booking.getCustomerName());
            invoice.setCreatedTime(java.time.LocalDateTime.now());
            invoice.setTotalAmount(totalAmount);
            invoice.setNote("Thanh toán đặt phòng + dịch vụ cho Booking #" + id + " (Phòng: " + roomTotal + " VNĐ, Dịch vụ: " + serviceTotal + " VNĐ)");
            invoice = invoiceRepository.save(invoice);

            // Gắn invoice cho các dịch vụ đã thanh toán
            for (ServiceOrder service : confirmedServices) {
                service.setInvoice(invoice);
                serviceOrderService.save(service);
            }

            // Cập nhật trạng thái booking
            String newStatus;
            if (booking.getStatus() != null && booking.getStatus().equals("Chờ xác nhận")) {
                newStatus = "Đã thanh toán - Chờ admin xác nhận";
            } else {
                newStatus = "Đã thanh toán phòng";
            }
            booking.setStatus(newStatus);
            bookingService.save(booking);
            
            redirectAttributes.addFlashAttribute("success", 
                "✅ Thanh toán thành công! Tổng: " + String.format("%,.0f", totalAmount) + " VNĐ" +
                " (Phòng: " + String.format("%,.0f", (double)roomTotal) + " VNĐ" +
                (serviceTotal > 0 ? " + Dịch vụ: " + String.format("%,.0f", serviceTotal) + " VNĐ)" : ")"));
            return "redirect:/booking/invoice/" + id;
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi xử lý thanh toán: " + e.getMessage());
            return "redirect:/khachhang/payment/" + id;
        }
    }

    // Thanh toán các dịch vụ đã được xác nhận (sau khi đã thanh toán tiền phòng)
    @PostMapping("/{id}/services")
    public String payConfirmedServices(@PathVariable Long id, HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán");
                return "redirect:/auth/login";
            }

            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy booking");
                return "redirect:/khachhang/services/my-orders";
            }

            // Quyền sở hữu
            boolean isOwner = (booking.getUserId() != null && booking.getUserId().equals(user.getId()))
                           || (booking.getCustomerName() != null && booking.getCustomerName().equalsIgnoreCase(user.getUsername()));
            if (!isOwner) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thanh toán dịch vụ cho booking này");
                return "redirect:/khachhang/services/my-orders?bookingId=" + id;
            }

            // Lấy danh sách dịch vụ đủ điều kiện thanh toán (chưa có invoice)
            java.util.List<ServiceOrder> toPay = serviceOrderService.findUnpaidConfirmedByBookingId(id);
            if (toPay.isEmpty()) {
                redirectAttributes.addFlashAttribute("info", "Không có dịch vụ đủ điều kiện thanh toán.");
                return "redirect:/booking/invoice/" + id;
            }

            double amount = toPay.stream().filter(o -> o.getTotalAmount() != null)
                                  .mapToDouble(ServiceOrder::getTotalAmount).sum();

            // Tạo hóa đơn dịch vụ phát sinh
            Invoice invoice = new Invoice();
            invoice.setCustomerName(booking.getCustomerName());
            invoice.setCreatedTime(java.time.LocalDateTime.now());
            invoice.setTotalAmount(amount);
            invoice.setNote("Thanh toán dịch vụ phát sinh cho Booking #" + id + " (" + toPay.size() + " dịch vụ)");
            invoice = invoiceRepository.save(invoice);

            // Gắn invoice cho từng dịch vụ
            for (ServiceOrder o : toPay) {
                o.setInvoice(invoice);
                serviceOrderService.save(o);
            }

            // Cập nhật status booking nếu tất cả đã thanh toán
            java.util.List<ServiceOrder> remainingUnpaid = serviceOrderService.findUnpaidConfirmedByBookingId(id);
            if (remainingUnpaid.isEmpty()) {
                if (booking.getStatus() != null && booking.getStatus().contains("Chờ admin xác nhận")) {
                    booking.setStatus("Đã thanh toán đầy đủ - Chờ admin xác nhận");
                } else {
                    booking.setStatus("Đã thanh toán đầy đủ");
                }
                bookingService.save(booking);
            }

            redirectAttributes.addFlashAttribute("success", 
                "✅ Thanh toán " + toPay.size() + " dịch vụ thành công! Tổng: " + String.format("%,.0f", amount) + " VNĐ");
            return "redirect:/booking/invoice/" + id;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi khi thanh toán dịch vụ: " + e.getMessage());
            return "redirect:/khachhang/services/my-orders?bookingId=" + id;
        }
    }
}
