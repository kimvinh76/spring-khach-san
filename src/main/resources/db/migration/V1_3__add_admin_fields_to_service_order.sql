-- Add admin processing fields to service_order table
ALTER TABLE service_order 
ADD COLUMN admin_note TEXT,
ADD COLUMN processed_by VARCHAR(255),
ADD COLUMN updated_at TIMESTAMP;
