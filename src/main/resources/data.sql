-- Thêm dữ liệu mẫu cho bảng room_type
INSERT INTO room_type (ten_loai, description, price_base, amenities, image) VALUES
('Phòng Tiêu Chuẩn', 'Phòng đơn giản với đầy đủ tiện nghi cơ bản', 500000, 'WiFi, TV, Điều hòa, Tủ lạnh mini', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=300&h=180&fit=crop'),
('Phòng Cao Cấp', 'Phòng rộng rãi với view đẹp và tiện nghi hiện đại', 800000, 'WiFi, Smart TV, Điều hòa, Tủ lạnh, Bồn tắm', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=300&h=180&fit=crop'),
('Phòng VIP', 'Phòng sang trọng với dịch vụ 5 sao', 1200000, 'WiFi, Smart TV 55 inch, Điều hòa, Minibar, Jacuzzi, Balcony', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=300&h=180&fit=crop');

-- Thêm dữ liệu mẫu cho bảng room
INSERT INTO room (name, room_type_id, capacity, description, image, available, status, created_at, updated_at) VALUES
('101', 1, 2, 'Phòng tiêu chuẩn tầng 1', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=300&h=200&fit=crop', true, 'available', NOW(), NOW()),
('102', 1, 2, 'Phòng tiêu chuẩn tầng 1', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=300&h=200&fit=crop', true, 'available', NOW(), NOW()),
('201', 2, 3, 'Phòng cao cấp tầng 2', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=300&h=200&fit=crop', true, 'available', NOW(), NOW()),
('202', 2, 3, 'Phòng cao cấp tầng 2', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=300&h=200&fit=crop', false, 'occupied', NOW(), NOW()),
('301', 3, 4, 'Phòng VIP tầng 3', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=300&h=200&fit=crop', true, 'available', NOW(), NOW());
