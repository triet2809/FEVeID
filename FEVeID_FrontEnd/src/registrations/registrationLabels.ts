import type { RegistrationStatus } from "../api/types";

// Record<enum, ...>: backend thêm trạng thái mới → sinh lại schema.d.ts → TS báo thiếu ở đây
export const REGISTRATION_STATUS_TAGS: Record<
  RegistrationStatus,
  { label: string; color: string }
> = {
  PENDING_PAYMENT: { label: "Chờ thanh toán", color: "gold" },
  CONFIRMED: { label: "Đã đăng ký", color: "blue" },
  CHECKED_IN: { label: "Đã check-in", color: "green" },
  CANCELLED: { label: "Đã hủy", color: "default" },
  EXPIRED: { label: "Hết hạn giữ chỗ", color: "default" },
  NO_SHOW: { label: "Không đến", color: "red" },
};

// Còn giữ chỗ (khớp RegistrationStatus.ACTIVE bên backend)
export function isActiveRegistration(status: RegistrationStatus): boolean {
  return (
    status === "PENDING_PAYMENT" ||
    status === "CONFIRMED" ||
    status === "CHECKED_IN"
  );
}
