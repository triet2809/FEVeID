-- Sổ cái điểm hoạt động (requirements.md 11, Bài 12.4). Giống sao kê ngân hàng: mỗi lần cộng/trừ là MỘT DÒNG MỚI,
-- không có cột "số dư" nào để sửa. Số dư kỳ = SUM(amount) của kỳ đó; sai thì thêm dòng điều chỉnh, không sửa dòng cũ
CREATE TABLE point_transactions(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    term_id BIGINT NOT NULL REFERENCES terms(id),
    amount INTEGER NOT NULL,             -- dương = cộng, âm = trừ
    type VARCHAR(20) NOT NULL,
    event_id BIGINT REFERENCES events(id), -- sự kiện nguồn (tổ chức/hỗ trợ/tham gia)
    reason VARCHAR(500),
    created_by_member_id BIGINT REFERENCES members(id), -- HR/BCN ghi; null = hệ thống tự ghi
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_point_tx_amount CHECK (amount <> 0),
    CONSTRAINT chk_point_tx_type CHECK (type IN
        ('ORGANIZE', 'SUPPORT', 'ATTEND', 'ADJUSTMENT', 'REDEEM', 'REFUND')),
    -- Điểm từ sự kiện phải ghi rõ sự kiện nào; loại khác thì không gắn sự kiện
    CONSTRAINT chk_point_tx_event CHECK ((type IN ('ORGANIZE', 'SUPPORT', 'ATTEND')) = (event_id IS NOT NULL)),
    -- Điều chỉnh tay bắt buộc có lý do (requirements 11.4)
    CONSTRAINT chk_point_tx_adjustment_reason CHECK (type <> 'ADJUSTMENT' OR reason IS NOT NULL)
);

-- Mỗi thành viên chỉ một vai trò, nhận điểm một lần trong một sự kiện (requirements 11.1).
-- Cũng là lưới an toàn khi chốt sự kiện (Bài 13) lỡ chạy 2 lần
CREATE UNIQUE INDEX uq_point_tx_event_member ON point_transactions (event_id, member_id)
    WHERE type IN ('ORGANIZE', 'SUPPORT', 'ATTEND');

-- Tính số dư theo kỳ + lịch sử của một thành viên
CREATE INDEX idx_point_tx_member_term ON point_transactions (member_id, term_id);

-- Chỉ thêm, không sửa/xóa (requirements 11.5), chặn luôn ở DB: kể cả code lỗi hay ai đó gõ SQL tay cũng không sửa được.
-- TRUNCATE không kích hoạt trigger theo dòng nên test vẫn dọn bảng được (ApiTestSupport.cleanDatabase)
CREATE FUNCTION forbid_point_transaction_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'point_transactions chỉ được thêm, không được sửa hoặc xóa';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_point_transactions_append_only
    BEFORE UPDATE OR DELETE ON point_transactions
    FOR EACH ROW EXECUTE FUNCTION forbid_point_transaction_change();
