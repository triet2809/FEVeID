-- Team sự kiện (requirements.md 5.2 "phân công thành viên từ nhiều ban vào team tổ chức", Bài 13.1).
-- Khi chốt sự kiện (Bài 13.2), danh sách này được tick sẵn: ORGANIZER +4, SUPPORTER +2
CREATE TABLE event_staff(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- ON DELETE CASCADE: xóa bản nháp sự kiện thì team đi theo (Event không giữ list team như event_phases)
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES members(id),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_event_staff_role CHECK (role IN ('ORGANIZER', 'SUPPORTER')),
    -- Mỗi người một vai trò trong một sự kiện (requirements 11.1)
    CONSTRAINT uq_event_staff_event_member UNIQUE (event_id, member_id)
);
