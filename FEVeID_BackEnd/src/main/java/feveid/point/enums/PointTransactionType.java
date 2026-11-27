package feveid.point.enums;

import java.util.Set;

// Nguồn của một giao dịch điểm (requirements.md 11.1, 11.3, 11.4)
public enum PointTransactionType {
    ORGANIZE,   // +4 tổ chức xong sự kiện (Bài 13)
    SUPPORT,    // +2 hỗ trợ xong sự kiện (Bài 13)
    ATTEND,     // +1 tham gia (check-in), phát khi chốt sự kiện (Bài 13)
    ADJUSTMENT, // HR/BCN cộng/trừ tay, bắt buộc có lý do
    REDEEM,     // trừ khi đổi quà (Shop, Bài 15)
    REFUND;     // hoàn khi hủy đổi quà (Shop, Bài 15)

    // Tiêu điểm/hoàn điểm ở shop KHÔNG tính vào tổng điểm trọn đời (requirements 11.2: chỉ tính điểm kiếm được)
    public static final Set<PointTransactionType> SHOP = Set.of(REDEEM, REFUND);
}
