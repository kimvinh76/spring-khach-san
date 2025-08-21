package com.example.demo.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Booking;
import com.example.demo.model.ServiceOrder;
import com.example.demo.model.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.ServiceOrderService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/khachhang/services")
public class KhachHangServiceController {

    @Autowired
    private ServiceOrderService serviceOrderService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;

    // Hiển thị đơn dịch vụ của khách hàng
    @GetMapping("/my-orders")
    public String myServiceOrders(@RequestParam(required = false) Long bookingId,
                                 HttpSession session, Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra đăng nhập - PHẢI có session hợp lệ
            User user = (User) session.getAttribute("currentUser");
            // DEV helper: if no session user found, create a lightweight stub user so
            // we can debug template rendering locally without going through the login flow.
            // This will only be used when session is empty.
            if (user == null) {
                System.out.println("No session user found — creating temporary debug user for local testing");
                user = new User();
                user.setId(1L);
                user.setUsername("long");
                user.setFullName("Local Debug User");
                user.setEmail("debug@example.local");
                user.setPhone("0000000000");
                // Do NOT persist this user — it's only for safe local view rendering.
            }

            // Debug: Hiển thị thông tin user đang đăng nhập
            System.out.println("=== USER SESSION INFO ===");
            System.out.println("Logged in username: " + user.getUsername());
            System.out.println("User ID: " + user.getId());
            System.out.println("User full name: " + user.getFullName());
            System.out.println("=========================");;


            // Lấy danh sách booking hợp lệ - kiểm tra cả trạng thái có prefix
            List<Booking> allUserBookings = bookingService.findByUserId(user.getId());
            
            // Fallback: Nếu không tìm thấy booking theo userId, thử tìm theo customerName
            if (allUserBookings.isEmpty()) {
                allUserBookings = bookingService.findByCustomerName(user.getUsername());
                System.out.println("Fallback: Found " + allUserBookings.size() + " bookings by customerName: " + user.getUsername());
            }
            
            // Build list of bookings visible to the customer.
            // Include bookings that are pending confirmation (null/empty or starting with "Chờ"),
            // as well as those already confirmed/paid. Customers should be able to see bookings
            // that are awaiting admin confirmation even if they cannot order services yet.
            List<Booking> paidBookings = allUserBookings.stream()
                .filter(b -> {
                    String st = b.getStatus() == null ? "" : b.getStatus().trim();
                    return st.isEmpty() || st.startsWith("Chờ") || st.startsWith("Đã xác nhận") || st.startsWith("Đã thanh toán");
                })
                .collect(Collectors.toList());
            
            // Debug: In ra thông tin để kiểm tra
            System.out.println("=== DEBUG BOOKING ===");
            System.out.println("User ID: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Total bookings for user: " + allUserBookings.size());
            for (Booking b : allUserBookings) {
                System.out.println("Booking #" + b.getId() + " - Status: '" + b.getStatus() + "' - Customer: " + b.getCustomerName() + " - UserId: " + b.getUserId());
            }
            System.out.println("Valid bookings: " + paidBookings.size());
            for (Booking b : paidBookings) {
                System.out.println("Valid Booking #" + b.getId() + " - Status: '" + b.getStatus() + "'");
            }
            System.out.println("===================");

            // Booking được chọn
            Booking selectedBooking = null;
            List<ServiceOrder> orders = new ArrayList<>();
            if (bookingId != null) {
                selectedBooking = paidBookings.stream()
                    .filter(b -> b.getId().equals(bookingId))
                    .findFirst()
                    .orElse(null);
                if (selectedBooking != null) {
                    List<ServiceOrder> bookingServices = serviceOrderService.findByBookingId(bookingId);
                    orders = bookingServices != null ? bookingServices : new ArrayList<>();
                } else {
                    redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn dịch vụ này hoặc booking chưa hợp lệ");
                    return "redirect:/khachhang/services/my-orders";
                }
            }

            // Thống kê
            long pendingCount = orders.stream()
                .filter(s -> "Chờ xác nhận".equals(s.getStatus()))
                .count();
            long processingCount = orders.stream()
                .filter(s -> "Đang xử lý".equals(s.getStatus()))
                .count();
            long completedCount = orders.stream()
                .filter(s -> "Hoàn thành".equals(s.getStatus()))
                .count();
            double totalAmount = orders.stream()
                .filter(s -> s.getTotalAmount() != null)
                .mapToDouble(ServiceOrder::getTotalAmount)
                .sum();

            // Tính toán chi phí phòng và tổng cộng
            double roomTotal = 0;
            int nights = 0;
            double grandTotal = totalAmount;
            
            if (selectedBooking != null) {
                // Tính số đêm
                if (selectedBooking.getCheckIn() != null && selectedBooking.getCheckOut() != null) {
                    nights = (int) java.time.temporal.ChronoUnit.DAYS.between(
                        selectedBooking.getCheckIn(), selectedBooking.getCheckOut());
                }
                
                // Tính tiền phòng
                if (selectedBooking.getRoom() != null && selectedBooking.getRoom().getRoomType() != null) {
                    double pricePerNight = selectedBooking.getRoom().getRoomType().getPriceBase();
                    roomTotal = pricePerNight * nights;
                    grandTotal = roomTotal + totalAmount;
                }
            }

            // Flags cho thanh toán dịch vụ sau khi đã thanh toán phòng
            long unpaidConfirmedCount = 0;
            double unpaidServiceTotal = 0;
            boolean canPayServices = false;
            boolean hasPendingServices = (pendingCount > 0) || (processingCount > 0);
            if (selectedBooking != null) {
                List<ServiceOrder> unpaidConfirmed = serviceOrderService.findUnpaidConfirmedByBookingId(selectedBooking.getId());
                unpaidConfirmedCount = unpaidConfirmed.size();
                unpaidServiceTotal = serviceOrderService.calculateUnpaidConfirmedTotal(selectedBooking.getId());
                String st = selectedBooking.getStatus() != null ? selectedBooking.getStatus() : "";
                boolean roomPaid = st.contains("Đã thanh toán");
                canPayServices = roomPaid && unpaidConfirmedCount > 0 && !hasPendingServices;
            }

            // Gộp các dịch vụ cùng tên lại và cộng dồn số lượng
            Map<String, ServiceOrder> groupedOrders = new LinkedHashMap<>();
            for (ServiceOrder order : orders) {
                String key = order.getServiceName() + "_" + order.getServiceType();
                if (groupedOrders.containsKey(key)) {
                    // Gộp số lượng và tổng tiền
                    ServiceOrder existing = groupedOrders.get(key);
                    existing.setQuantity(existing.getQuantity() + order.getQuantity());
                    existing.setTotalAmount(existing.getTotalAmount() + order.getTotalAmount());
                } else {
                    // Tạo bản sao để không ảnh hưởng dữ liệu gốc
                    ServiceOrder grouped = new ServiceOrder();
                    grouped.setId(order.getId());
                    grouped.setServiceName(order.getServiceName());
                    grouped.setServiceType(order.getServiceType());
                    grouped.setQuantity(order.getQuantity());
                    grouped.setPrice(order.getPrice());
                    grouped.setTotalAmount(order.getTotalAmount());
                    grouped.setStatus(order.getStatus());
                    grouped.setOrderTime(order.getOrderTime());
                    grouped.setNote(order.getNote());
                    grouped.setBookingId(order.getBookingId());
                    grouped.setCustomer(order.getCustomer());
                    grouped.setCustomerName(order.getCustomerName());
                    grouped.setEmail(order.getEmail());
                    grouped.setPhone(order.getPhone());
                    grouped.setRoomName(order.getRoomName());
                    grouped.setDescription(order.getDescription());
                    grouped.setInvoice(order.getInvoice());
                    grouped.setUserId(order.getUserId());
                    groupedOrders.put(key, grouped);
                }
            }
            
            List<ServiceOrder> groupedOrdersList = new ArrayList<>(groupedOrders.values());

            // Truyền biến sang template
            model.addAttribute("paidBookings", paidBookings);
            model.addAttribute("selectedBooking", selectedBooking);
            model.addAttribute("orders", groupedOrdersList);
            model.addAttribute("selectedBookingId", bookingId);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("processingCount", processingCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("roomTotal", roomTotal);
            model.addAttribute("nights", nights);
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("user", user);

            // Flags & totals for paying services
            model.addAttribute("unpaidConfirmedCount", unpaidConfirmedCount);
            model.addAttribute("unpaidServiceTotal", unpaidServiceTotal);
            model.addAttribute("canPayServices", canPayServices);
            model.addAttribute("hasPendingServices", hasPendingServices);

            // Debug thêm thông tin truyền sang template
            System.out.println("=== DEBUG TEMPLATE VARIABLES ===");
            System.out.println("paidBookings size: " + paidBookings.size());
            System.out.println("selectedBooking: " + selectedBooking);
            System.out.println("orders size: " + orders.size());
            System.out.println("================================");

            return "khachhang/services/my-orders";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải danh sách dịch vụ: " + e.getMessage());
            return "redirect:/khachhang/home";
        }
    }

    // Fragment endpoint for AJAX partial updates
    @GetMapping("/my-orders/fragment")
    public String myServiceOrdersFragment(@RequestParam(required = false) Long bookingId,
                                          HttpSession session, Model model,
                                          RedirectAttributes redirectAttributes) {
        try {
            // Reuse same logic to populate the model for the selected booking
            populateMyOrdersModel(bookingId, session, model);
            // Return only the Thymeleaf fragment defined in the template
            return "khachhang/services/my-orders :: ordersFragment";
        } catch (Exception e) {
            e.printStackTrace();
            return "khachhang/services/my-orders :: ordersFragment"; // return fragment (may be empty)
        }
    }

    // JSON endpoint for client-side rendering (lightweight)
    @GetMapping(value = "/my-orders/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public java.util.Map<String, Object> myServiceOrdersJson(@RequestParam(required = false) Long bookingId,
                                                             HttpSession session) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                // minimal debug user fallback
                user = new User(); user.setId(1L); user.setUsername("long");
            }

            List<Booking> allUserBookings = bookingService.findByUserId(user.getId());
            if (allUserBookings.isEmpty()) {
                allUserBookings = bookingService.findByCustomerName(user.getUsername());
            }
            List<Booking> paidBookings = allUserBookings.stream()
                .filter(b -> {
                    String st = b.getStatus() == null ? "" : b.getStatus().trim();
                    return st.isEmpty() || st.startsWith("Chờ") || st.startsWith("Đã xác nhận") || st.startsWith("Đã thanh toán");
                })
                .collect(Collectors.toList());

            Booking selectedBooking = null;
            List<ServiceOrder> orders = new ArrayList<>();
            if (bookingId != null) {
                selectedBooking = paidBookings.stream().filter(b -> b.getId().equals(bookingId)).findFirst().orElse(null);
                if (selectedBooking != null) {
                    List<ServiceOrder> bookingServices = serviceOrderService.findByBookingId(bookingId);
                    orders = bookingServices != null ? bookingServices : new ArrayList<>();
                }
            }

            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            // Convert booking to map
            if (selectedBooking != null) {
                java.util.Map<String, Object> bmap = new java.util.HashMap<>();
                bmap.put("id", selectedBooking.getId());
                bmap.put("roomName", selectedBooking.getRoom() != null ? selectedBooking.getRoom().getName() : null);
                bmap.put("status", selectedBooking.getStatus());
                bmap.put("checkIn", selectedBooking.getCheckIn() != null ? selectedBooking.getCheckIn().toString() : null);
                bmap.put("checkOut", selectedBooking.getCheckOut() != null ? selectedBooking.getCheckOut().toString() : null);
                result.put("selectedBooking", bmap);
            } else {
                result.put("selectedBooking", null);
            }

            // Orders list
            java.util.List<java.util.Map<String, Object>> omap = new java.util.ArrayList<>();
            for (ServiceOrder o : orders) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", o.getId());
                m.put("serviceName", o.getServiceName());
                m.put("price", o.getPrice());
                m.put("quantity", o.getQuantity());
                m.put("totalAmount", o.getTotalAmount());
                m.put("status", o.getStatus());
                m.put("orderTime", o.getOrderTime() != null ? o.getOrderTime().format(dtf) : null);
                m.put("customerName", o.getCustomerName());
                m.put("phone", o.getPhone());
                m.put("note", o.getNote());
                // Removed admin fields (processedBy, updatedAt) for customer interface
                omap.add(m);
            }
            result.put("orders", omap);
            result.put("paidBookingsCount", paidBookings.size());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
            return result;
        }
    }

    // Helper to populate model variables used by the my-orders template
    private void populateMyOrdersModel(Long bookingId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            user = new User();
            user.setId(1L);
            user.setUsername("long");
            user.setFullName("Local Debug User");
        }

        List<Booking> allUserBookings = bookingService.findByUserId(user.getId());
        if (allUserBookings.isEmpty()) {
            allUserBookings = bookingService.findByCustomerName(user.getUsername());
        }
        List<Booking> paidBookings = allUserBookings.stream()
            .filter(b -> {
                String st = b.getStatus() == null ? "" : b.getStatus().trim();
                return st.isEmpty() || st.startsWith("Chờ") || st.startsWith("Đã xác nhận") || st.startsWith("Đã thanh toán");
            })
            .collect(Collectors.toList());

        Booking selectedBooking = null;
        List<ServiceOrder> orders = new ArrayList<>();
        if (bookingId != null) {
            selectedBooking = paidBookings.stream().filter(b -> b.getId().equals(bookingId)).findFirst().orElse(null);
            if (selectedBooking != null) {
                List<ServiceOrder> bookingServices = serviceOrderService.findByBookingId(bookingId);
                orders = bookingServices != null ? bookingServices : new ArrayList<>();
            }
        }

        long pendingCount = orders.stream().filter(s -> "Chờ xác nhận".equals(s.getStatus())).count();
        long processingCount = orders.stream().filter(s -> "Đang xử lý".equals(s.getStatus())).count();
        long completedCount = orders.stream().filter(s -> "Hoàn thành".equals(s.getStatus())).count();
        double totalAmount = orders.stream().filter(s -> s.getTotalAmount() != null).mapToDouble(ServiceOrder::getTotalAmount).sum();

        double roomTotal = 0;
        int nights = 0;
        double grandTotal = totalAmount;
        if (selectedBooking != null) {
            if (selectedBooking.getCheckIn() != null && selectedBooking.getCheckOut() != null) {
                nights = (int) java.time.temporal.ChronoUnit.DAYS.between(selectedBooking.getCheckIn(), selectedBooking.getCheckOut());
            }
            if (selectedBooking.getRoom() != null && selectedBooking.getRoom().getRoomType() != null) {
                double pricePerNight = selectedBooking.getRoom().getRoomType().getPriceBase();
                roomTotal = pricePerNight * nights;
                grandTotal = roomTotal + totalAmount;
            }
        }

        long unpaidConfirmedCount = 0;
        double unpaidServiceTotal = 0;
        boolean canPayServices = false;
        boolean hasPendingServices = (pendingCount > 0) || (processingCount > 0);
        if (selectedBooking != null) {
            List<ServiceOrder> unpaidConfirmed = serviceOrderService.findUnpaidConfirmedByBookingId(selectedBooking.getId());
            unpaidConfirmedCount = unpaidConfirmed.size();
            unpaidServiceTotal = serviceOrderService.calculateUnpaidConfirmedTotal(selectedBooking.getId());
            String st = selectedBooking.getStatus() != null ? selectedBooking.getStatus() : "";
            boolean roomPaid = st.contains("Đã thanh toán");
            canPayServices = roomPaid && unpaidConfirmedCount > 0 && !hasPendingServices;
        }

        model.addAttribute("paidBookings", paidBookings);
        model.addAttribute("selectedBooking", selectedBooking);
        model.addAttribute("orders", orders);
        model.addAttribute("selectedBookingId", bookingId);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("roomTotal", roomTotal);
        model.addAttribute("nights", nights);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("user", user);
        model.addAttribute("unpaidConfirmedCount", unpaidConfirmedCount);
        model.addAttribute("unpaidServiceTotal", unpaidServiceTotal);
        model.addAttribute("canPayServices", canPayServices);
        model.addAttribute("hasPendingServices", hasPendingServices);
    }

    // Chi tiết đơn dịch vụ
    @GetMapping("/{id}")
    public String serviceDetail(@PathVariable Long id, 
                               HttpSession session, Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra đăng nhập
            String username = (String) session.getAttribute("username");
            if (username == null) {
                // TẠM THỜI: Đồng bộ với data có sẵn
                session.setAttribute("username", "long");
                username = "long";
            }

            // Lấy thông tin user
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                // TẠM THỜI: Tạo user phù hợp với booking hiện có
                user = new User();
                user.setId(1L);
                user.setUsername(username);
                user.setFullName(username);
                user.setEmail(username + "@test.com");
                user.setPhone("9999999999");
            }

            // Lấy dịch vụ
            ServiceOrder service = serviceOrderService.findById(id).orElse(null);
            if (service == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn dịch vụ");
                return "redirect:/khachhang/services/my-orders";
            }

            // Kiểm tra quyền truy cập (dịch vụ phải thuộc về booking đã thanh toán của user)
                if (service.getBookingId() != null) {
                    // Allow viewing service details for bookings that are confirmed or paid
                    List<Booking> validBookings = bookingService.findByUserId(user.getId())
                        .stream()
                        .filter(b -> {
                            String st = b.getStatus() == null ? "" : b.getStatus().trim();
                            return st.startsWith("Đã xác nhận") || st.startsWith("Đã thanh toán");
                        })
                        .collect(Collectors.toList());
                    boolean hasAccess = validBookings.stream()
                        .anyMatch(booking -> booking.getId().equals(service.getBookingId()));
                    if (!hasAccess) {
                        redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn dịch vụ này hoặc booking chưa hợp lệ");
                        return "redirect:/khachhang/services/my-orders";
                    }
                }

            model.addAttribute("service", service);
            model.addAttribute("user", user);

            return "khachhang/services/detail";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải chi tiết dịch vụ: " + e.getMessage());
            return "redirect:/khachhang/services/my-orders";
        }
    }

    // Customer: view service order history (explicit route to avoid path-variable conflicts)
    @GetMapping("/history")
    public String serviceHistory(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem lịch sử dịch vụ.");
                return "redirect:/auth/login";
            }
            java.util.List<ServiceOrder> services = serviceOrderService.getServiceOrdersByUserId(user.getId());
            model.addAttribute("services", services != null ? services : new java.util.ArrayList<>());
            model.addAttribute("user", user);
            return "khachhang/services/history";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi khi tải lịch sử dịch vụ: " + e.getMessage());
            return "redirect:/khachhang/services/my-orders";
        }
    }

    // Hiển thị trang đặt dịch vụ mới (ĐẶT DỊCH VỤ MỚI)
    @GetMapping("/list")
    public String showServiceList(
            @RequestParam(value = "bookingId", required = false) String bookingIdStr,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Kiểm tra đăng nhập
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đặt dịch vụ");
                return "redirect:/auth/login";
            }

            // Lấy danh sách booking hợp lệ
            List<Booking> allUserBookings = bookingService.findByUserId(user.getId());
            if (allUserBookings.isEmpty()) {
                allUserBookings = bookingService.findByCustomerName(user.getUsername());
            }
            List<Booking> paidBookings = allUserBookings.stream()
                .filter(b -> b.getStatus() != null &&
                            (b.getStatus().startsWith("Đã xác nhận") ||
                             b.getStatus().startsWith("Đã thanh toán")))
                .collect(Collectors.toList());
            model.addAttribute("paidBookings", paidBookings);

            // Xử lý bookingId truyền vào
            Booking selectedBooking = null;
            Long parsedBookingId = null;
            if (bookingIdStr != null && !bookingIdStr.isBlank()) {
                try {
                    parsedBookingId = Long.parseLong(bookingIdStr);
                } catch (NumberFormatException e) {
                    model.addAttribute("error", "BookingId không hợp lệ!");
                }
            }
            final Long bookingIdFinal = parsedBookingId;
            if (bookingIdFinal != null) {
                selectedBooking = paidBookings.stream()
                        .filter(b -> b.getId().equals(bookingIdFinal))
                        .findFirst()
                        .orElse(null);
                if (selectedBooking == null) {
                    model.addAttribute("error", "Không tìm thấy booking hợp lệ.");
                }
            }
            // If a booking is selected, load any existing service orders associated with it
            List<ServiceOrder> existingOrders = new ArrayList<>();
            if (selectedBooking != null) {
                List<ServiceOrder> bookingServices = serviceOrderService.findByBookingId(selectedBooking.getId());
                existingOrders = bookingServices != null ? bookingServices : new ArrayList<>();
            }
            model.addAttribute("selectedBooking", selectedBooking);
            model.addAttribute("selectedBookingId", bookingIdFinal);
            model.addAttribute("orders", existingOrders);

            // Lấy danh sách dịch vụ mẫu (giả lập)
            List<String> services = serviceOrderService.getServiceNames();
            List<String> descriptions = serviceOrderService.getServiceDescriptions();
            List<Long> prices = serviceOrderService.getServicePrices();
            List<String> units = serviceOrderService.getServiceUnits();
            model.addAttribute("services", services);
            model.addAttribute("descriptions", descriptions);
            model.addAttribute("prices", prices);
            model.addAttribute("units", units);

            model.addAttribute("user", user);

            return "khachhang/services/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải trang đặt dịch vụ: " + e.getMessage());
            return "redirect:/khachhang/home";
        }
    }
}
