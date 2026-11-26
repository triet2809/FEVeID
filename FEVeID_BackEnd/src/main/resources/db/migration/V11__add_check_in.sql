-- Check-in (requirements.md 12.3, Bài 12).

-- 1. Bí mật để tạo mã QR xoay vòng (CheckInCodes): 32 byte ngẫu nhiên, mỗi sự kiện một cái,
--    lộ của sự kiện này không ảnh hưởng sự kiện khác. Sự kiện mới do Java tạo (Event.@PrePersist).
--    Sự kiện đã có: ghép 2 UUID ngẫu nhiên (gen_random_uuid có sẵn từ Postgres 13) -> 32 byte
ALTER TABLE events ADD COLUMN check_in_secret BYTEA;
UPDATE events SET check_in_secret =
    decode(replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''), 'hex');
ALTER TABLE events ALTER COLUMN check_in_secret SET NOT NULL;

-- 2. Ai check-in, lúc nào. checked_in_by_member_id = null: tự quét QR; có giá trị: HR/BCN check-in tay
ALTER TABLE registrations ADD COLUMN checked_in_at TIMESTAMPTZ;
ALTER TABLE registrations ADD COLUMN checked_in_by_member_id BIGINT REFERENCES members(id);

-- Đã CHECKED_IN thì phải có giờ check-in, trạng thái khác thì không. Sau này NO_SHOW chỉ áp cho người chưa check-in
ALTER TABLE registrations ADD CONSTRAINT chk_registrations_checked_in
    CHECK ((status = 'CHECKED_IN') = (checked_in_at IS NOT NULL));
