-- Đăng ký tham gia sự kiện (requirements.md 12.2). Một dòng = một người đăng ký một sự kiện.
-- Người đăng ký là thành viên (member_id) HOẶC khách không có tài khoản (các cột guest_*, dùng ở Bài 11.3)
CREATE TABLE registrations(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    member_id BIGINT REFERENCES members(id),
    guest_name VARCHAR(100),
    guest_email VARCHAR(255),
    guest_phone VARCHAR(20),
    guest_school VARCHAR(200),  -- MSSV hoặc tên trường
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT chk_registrations_status CHECK ( status IN
        ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'CANCELLED', 'EXPIRED', 'NO_SHOW') ),
    -- Đúng một trong hai: thành viên (không có thông tin khách) hoặc khách (có tên + email, không có member_id)
    CONSTRAINT chk_registrations_registrant CHECK (
        (member_id IS NOT NULL AND guest_email IS NULL AND guest_name IS NULL)
        OR (member_id IS NULL AND guest_email IS NOT NULL AND guest_name IS NOT NULL) )
);

-- Mỗi người chỉ có 1 đăng ký còn hiệu lực trong 1 sự kiện; đã hủy/hết hạn thì đăng ký lại được.
-- Index "một phần" (WHERE): chỉ các dòng còn hiệu lực mới bị bắt trùng. Đây là lưới an toàn cuối cùng,
-- service vẫn phải tự kiểm tra trước để trả lỗi 409 dễ hiểu
CREATE UNIQUE INDEX uq_registrations_event_member_active ON registrations (event_id, member_id)
    WHERE member_id IS NOT NULL AND status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN');
CREATE UNIQUE INDEX uq_registrations_event_guest_active ON registrations (event_id, lower(guest_email))
    WHERE guest_email IS NOT NULL AND status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN');

-- "Sự kiện của tôi" (Bài 11.4)
CREATE INDEX idx_registrations_member ON registrations (member_id);
