import type { PointTransactionType } from "../api/types";

// Record<enum, ...>: backend thêm loại mới → sinh lại schema.d.ts → TS báo thiếu ở đây
export const POINT_TYPE_TAGS: Record<
  PointTransactionType,
  { label: string; color: string }
> = {
  ORGANIZE: { label: "Tổ chức", color: "purple" },
  SUPPORT: { label: "Hỗ trợ", color: "blue" },
  ATTEND: { label: "Tham gia", color: "green" },
  ADJUSTMENT: { label: "Điều chỉnh", color: "orange" },
  REDEEM: { label: "Đổi quà", color: "magenta" },
  REFUND: { label: "Hoàn điểm", color: "cyan" },
};

// +4 / -3 (luôn có dấu để nhìn là biết cộng hay trừ)
export function signed(amount: number): string {
  return amount > 0 ? `+${amount}` : String(amount);
}
