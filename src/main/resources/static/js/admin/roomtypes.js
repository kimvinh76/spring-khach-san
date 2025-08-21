/**
 * Room Types Management JavaScript
 * Tương thích với Spring Boot + Thymeleaf + AdminLTE
 */

// Khởi tạo khi DOM ready
$(document).ready(function() {
    console.log('Room Types page loaded');
    initializeTable();
});

// Hàm tìm kiếm đơn giản
function searchTable() {
    const input = document.getElementById("searchInput");
    const filter = input.value.toUpperCase();
    const table = document.getElementById("roomTypeTable");
    const rows = table.getElementsByTagName("tr");
    
    // Bắt đầu từ row 1 (bỏ qua header)
    for (let i = 1; i < rows.length; i++) {
        let found = false;
        const cells = rows[i].getElementsByTagName("td");
        
        // Kiểm tra tất cả các cell trong row
        for (let j = 0; j < cells.length; j++) {
            if (cells[j]) {
                const textValue = cells[j].textContent || cells[j].innerText;
                if (textValue.toUpperCase().indexOf(filter) > -1) {
                    found = true;
                    break;
                }
            }
        }
        
        // Hiển thị hoặc ẩn row
        rows[i].style.display = found ? "" : "none";
    }
}

// Hàm xác nhận xóa với SweetAlert2
function confirmDelete(id, name) {
    // Kiểm tra SweetAlert2 có load không
    if (typeof Swal === 'undefined') {
        if (confirm(`Bạn có chắc muốn xóa loại phòng "${name}"?`)) {
            window.location.href = '/admin/roomtypes/delete/' + id;
        }
        return;
    }
    
    Swal.fire({
        title: 'Xác nhận xóa',
        text: `Bạn có chắc muốn xóa loại phòng "${name}"?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = '/admin/roomtypes/delete/' + id;
        }
    });
}

// Hàm xem chi tiết với SweetAlert2  
function viewRoomType(id, name, description, price, amenities) {
    // Fallback nếu SweetAlert2 chưa load
    if (typeof Swal === 'undefined') {
        alert(`ID: ${id}\nTên: ${name}\nMô tả: ${description || 'Không có'}\nGiá: ${price}\nTiện nghi: ${amenities || 'Không có'}`);
        return;
    }
    
    Swal.fire({
        title: '<i class="fas fa-layer-group mr-2"></i>' + name,
        html: `
            <div class="text-left">
                <p><strong>ID:</strong> <span class="badge badge-primary">${id}</span></p>
                <p><strong>Mô tả:</strong> ${description || 'Không có mô tả'}</p>
                <p><strong>Giá cơ bản:</strong> <span class="text-success">${formatPrice(price)} VNĐ</span></p>
                <p><strong>Tiện nghi:</strong> ${amenities || 'Chưa có tiện nghi'}</p>
            </div>
        `,
        width: 600,
        confirmButtonText: 'Đóng'
    });
}

// Hàm format giá tiền
function formatPrice(price) {
    if (!price) return '0';
    return parseFloat(price).toLocaleString('vi-VN');
}

// Hàm khởi tạo table
function initializeTable() {
    const table = document.getElementById('roomTypeTable');
    if (!table) {
        console.log('Table not found');
        return;
    }
    
    console.log('Table initialized successfully');
}

// Export functions để có thể gọi từ HTML
window.searchTable = searchTable;
window.confirmDelete = confirmDelete;
window.viewRoomType = viewRoomType;
