-- Tạo bảng hóa đơn và chi tiết hóa đơn cho PostgreSQL
CREATE TABLE IF NOT EXISTS invoice (
    id SERIAL PRIMARY KEY,
    customer_name VARCHAR(255),
    total_amount DOUBLE PRECISION,
    created_time TIMESTAMP,
    note TEXT
);

CREATE TABLE IF NOT EXISTS invoice_detail (
    id SERIAL PRIMARY KEY,
    invoice_id INTEGER REFERENCES invoice(id) ON DELETE CASCADE,
    room_name VARCHAR(255),
    room_type VARCHAR(255),
    price DOUBLE PRECISION,
    nights INTEGER,
    amount DOUBLE PRECISION
);
