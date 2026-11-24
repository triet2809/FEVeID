CREATE TABLE events(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    -- URL ảnh trên Cloudinary, gán qua API upload (Bài 10.3)
    banner_url VARCHAR(500),
    location VARCHAR(300),
    -- INTERNAL: chỉ thành viên đăng ký; PUBLIC: cả người ngoài (requirements.md 12.1)
    visibility VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    -- Trạng thái do HR/BCN bấm; UPCOMING/ONGOING/COMPLETED tính từ event_phases (requirements.md 6.1)
    publication VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    -- Số người đăng ký tối đa; NULL = không giới hạn
    capacity INTEGER,
    registration_opens_at TIMESTAMPTZ,
    registration_closes_at TIMESTAMPTZ,
    -- Sự kiện thuộc cả CLB, không thuộc ban nào. Người phụ trách: leader do BCN gán sau khi chọn team,
    -- hoặc người gửi Event Proposal (sự kiện tự phát). NULL = chưa gán
    owner_member_id BIGINT REFERENCES members(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_events_visibility CHECK ( visibility IN ('INTERNAL', 'PUBLIC') ),
    CONSTRAINT chk_events_publication CHECK ( publication IN ('DRAFT', 'PUBLISHED', 'CANCELLED') ),
    CONSTRAINT chk_events_capacity CHECK ( capacity IS NULL OR capacity > 0 ),
    CONSTRAINT chk_events_registration CHECK ( registration_closes_at > registration_opens_at )
);

CREATE TABLE event_phases(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    kind VARCHAR(20) NOT NULL,
    CONSTRAINT chk_event_phases_kind CHECK ( kind IN ('UPCOMING', 'ONGOING', 'COMPLETED') )
);
-- Không đặt UNIQUE (event_id, starts_at): khi thay cả lịch, Hibernate INSERT giai đoạn mới trước rồi mới DELETE
-- giai đoạn cũ, giữ nguyên một mốc giờ sẽ bị trùng tạm thời. "Không trùng giờ" do PhaseScheduleValidator kiểm tra
CREATE INDEX idx_event_phases_event ON event_phases (event_id, starts_at);
