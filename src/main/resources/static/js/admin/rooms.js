/**
 * Rooms Management JavaScript  
 * Tương thích với Spring Boot + Thymeleaf + AdminLTE
 */

// Khởi tạo khi DOM ready
$(document).ready(function() {
    console.log('Rooms page loaded');
    initializeTable();
});

// Hàm tìm kiếm đơn giản
function searchTable() {
    const input = document.getElementById("searchInput");
    const filter = input.value.toUpperCase();
    const table = document.getElementById("roomTable");
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
function confirmDelete(id, roomNumber) {
    // Kiểm tra SweetAlert2 có load không
    if (typeof Swal === 'undefined') {
        if (confirm(`Bạn có chắc muốn xóa phòng "${roomNumber}"?`)) {
            window.location.href = '/admin/rooms/delete/' + id;
        }
        return;
    }
    
    Swal.fire({
        title: 'Xác nhận xóa',
        text: `Bạn có chắc muốn xóa phòng "${roomNumber}"?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = '/admin/rooms/delete/' + id;
        }
    });
}

// Hàm xem chi tiết phòng
function viewRoom(id, roomNumber, roomType, status, createdAt) {
    // Fallback nếu SweetAlert2 chưa load
    if (typeof Swal === 'undefined') {
        alert(`ID: ${id}\nSố phòng: ${roomNumber}\nLoại phòng: ${roomType || 'N/A'}\nTrạng thái: ${status}\nNgày tạo: ${createdAt || 'N/A'}`);
        return;
    }
    
    Swal.fire({
        title: '<i class="fas fa-bed mr-2"></i>Phòng ' + roomNumber,
        html: `
            <div class="text-left">
                <p><strong>ID:</strong> <span class="badge badge-primary">${id}</span></p>
                <p><strong>Số phòng:</strong> ${roomNumber}</p>
                <p><strong>Loại phòng:</strong> ${roomType || 'N/A'}</p>
                <p><strong>Trạng thái:</strong> <span class="badge ${getStatusBadgeClass(status)}">${getStatusText(status)}</span></p>
                <p><strong>Ngày tạo:</strong> ${formatDate(createdAt)}</p>
            </div>
        `,
        width: 500,
        confirmButtonText: 'Đóng'
    });
}

// Hàm lấy class CSS cho badge trạng thái
function getStatusBadgeClass(status) {
    switch(status) {
        case 'AVAILABLE': return 'badge-success';
        case 'OCCUPIED': return 'badge-danger';
        case 'MAINTENANCE': return 'badge-warning';
        default: return 'badge-secondary';
    }
}

// Hàm lấy text tiếng Việt cho trạng thái
function getStatusText(status) {
    switch(status) {
        case 'AVAILABLE': return 'Có sẵn';
        case 'OCCUPIED': return 'Đã đặt';
        case 'MAINTENANCE': return 'Bảo trì';
        default: return status;
    }
}

// Hàm format ngày tháng
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN');
    } catch (e) {
        return dateString;
    }
}

// Hàm khởi tạo table
function initializeTable() {
    const table = document.getElementById('roomTable');
    if (!table) {
        console.log('Room table not found');
        return;
    }
    
    console.log('Room table initialized successfully');
}

// Export functions để có thể gọi từ HTML
window.searchTable = searchTable;
window.confirmDelete = confirmDelete;
window.viewRoom = viewRoom;
