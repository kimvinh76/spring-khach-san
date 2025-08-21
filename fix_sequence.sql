-- Sửa sequence để tránh conflict primary key
-- Kiểm tra id cao nhất hiện tại
SELECT MAX(id) FROM room_type;

-- Reset sequence về giá trị cao hơn id lớn nhất
SELECT setval('room_type_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM room_type), false);

-- Xác nhận sequence đã được reset
SELECT nextval('room_type_id_seq');
