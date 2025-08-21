# 🏨 Hướng dẫn quy trình đặt phòng và dịch vụ khách sạn

## 📋 Quy trình tổng quan

### 1. **Đặt phòng (Booking)**
```
Khách hàng đặt phòng → [Chờ xác nhận] → Admin xác nhận → [Đã xác nhận] → Khách thanh toán → [Đã thanh toán]
```

### 2. **Đặt dịch vụ (Service Order)**
```
Booking [Đã xác nhận/Đã thanh toán] → Khách đặt dịch vụ → [Chờ xác nhận] → Admin xác nhận → [Đang xử lý] → [Hoàn thành]
```

---

## 🔄 Chi tiết từng bước

### **BƯỚC 1: Khách hàng đặt phòng**
- **Trạng thái:** `Chờ xác nhận`
- **Khách hàng có thể:** Xem lịch sử booking, chờ admin duyệt
- **Admin cần làm:** Vào `/admin/bookings` để xác nhận hoặc từ chối

### **BƯỚC 2: Admin xác nhận booking**
- **Nếu duyệt:** Trạng thái chuyển thành `Đã xác nhận`
- **Nếu từ chối:** Trạng thái chuyển thành `Đã hủy`
- **Khách hàng nhận được:** Thông báo kết quả

### **BƯỚC 3: Khách hàng thanh toán**
- **Điều kiện:** Booking phải ở trạng thái `Đã xác nhận`
- **Sau thanh toán:** Trạng thái chuyển thành `Đã thanh toán`
- **Mở khóa:** Khách có thể đặt dịch vụ

### **BƯỚC 4: Khách hàng đặt dịch vụ**
- **Điều kiện:** Booking phải `Đã xác nhận` hoặc `Đã thanh toán`
- **Trạng thái dịch vụ:** `Chờ xác nhận`
- **Đường dẫn:** `/services/my-orders?bookingId=<ID>`

### **BƯỚC 5: Admin xử lý dịch vụ**
- **Admin vào:** `/admin/services`
- **Có thể:** 
  - ✅ Xác nhận → `Đang xử lý`
  - ❌ Từ chối → `Từ chối - [lý do]`
  - ✅ Hoàn thành → `Hoàn thành`

---

## ❓ Câu hỏi thường gặp

### **Q: Admin có cần xác nhận dịch vụ riêng không?**
**A: CÓ!** Dịch vụ có quy trình xác nhận độc lập với booking:
- **Booking xác nhận:** Chỉ cho phép khách đặt dịch vụ
- **Dịch vụ xác nhận:** Admin kiểm soát việc thực hiện dịch vụ

### **Q: Khách có thể đặt dịch vụ khi nào?**
**A:** Khi booking ở trạng thái:
- ✅ `Đã xác nhận` (chưa thanh toán nhưng admin đã duyệt)
- ✅ `Đã thanh toán` (đã hoàn tất thanh toán)

### **Q: Nếu admin từ chối booking thì sao?**
**A:** 
- Booking chuyển thành `Đã hủy`
- Khách không thể thanh toán hoặc đặt dịch vụ
- Cần đặt phòng mới

### **Q: Tại sao cần 2 bước xác nhận?**
**A:** Để kiểm soát tốt hơn:
- **Booking:** Kiểm tra tình trạng phòng, thông tin khách
- **Dịch vụ:** Kiểm tra khả năng thực hiện, lên lịch nhân viên

---

## 🚨 Lưu ý quan trọng

1. **Booking phải được admin xác nhận trước** khi khách có thể đặt dịch vụ
2. **Mỗi dịch vụ cần xác nhận riêng** từ admin
3. **Khách có thể xem lịch sử** booking và dịch vụ để theo dõi tiến độ
4. **Admin quản lý 2 trang riêng biệt:**
   - `/admin/bookings` - Quản lý đặt phòng
   - `/admin/services` - Quản lý đơn dịch vụ

---

## 🔧 Đã sửa lỗi

✅ **Lỗi màn hình trắng trang lịch sử booking**
- Sửa lỗi `booking.room.roomType.name` → `booking.room.roomType.tenLoai`
- Thêm tìm kiếm theo tên khách hàng
- Cải thiện UX với hướng dẫn trạng thái

✅ **Cải thiện trải nghiệm khách hàng**
- Hiển thị rõ ràng trạng thái booking
- Hướng dẫn sử dụng chi tiết
- Thông báo khi không tìm thấy booking
