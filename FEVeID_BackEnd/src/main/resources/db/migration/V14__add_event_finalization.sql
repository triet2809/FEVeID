-- Chốt sự kiện (requirements.md 11.1, Bài 13.2): HR/BCN xác nhận team + người tham gia -> cộng điểm hàng loạt
-- và khóa sự kiện. Chốt 1 lần, không mở lại (phát sai thì sửa bằng dòng ADJUSTMENT trong sổ cái)
ALTER TABLE events ADD COLUMN finalized_at TIMESTAMPTZ;
ALTER TABLE events ADD COLUMN finalized_by_member_id BIGINT REFERENCES members(id);

-- Đã chốt thì phải biết ai chốt, chưa chốt thì cả hai đều trống
ALTER TABLE events ADD CONSTRAINT chk_events_finalized
    CHECK ((finalized_at IS NULL) = (finalized_by_member_id IS NULL));
