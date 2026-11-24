-- Sự kiện INTERNAL (chỉ thành viên đăng ký) vẫn có thể hiện cho khách xem nếu BCN chọn, để quảng bá CLB.
-- Sự kiện PUBLIC luôn hiện cho khách, cột này chỉ có ý nghĩa với INTERNAL
ALTER TABLE events ADD COLUMN publicly_listed BOOLEAN NOT NULL DEFAULT FALSE;
