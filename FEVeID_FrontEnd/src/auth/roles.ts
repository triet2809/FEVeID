import type { AccountRole } from "../api/types";

// Tên hiển thị cho từng role toàn CLB
export const ROLE_LABELS: Record<AccountRole, string> = {
  MEMBER: "Thành viên",
  HR: "HR",
  PRESIDENT: "Ban Chủ nhiệm",
};

// Nhóm role dùng chung cho CẢ menu lẫn route: sửa một chỗ → menu và chặn URL đổi theo.
// Phải khớp với @PreAuthorize ở backend (backend mới là nơi chặn thật, frontend chỉ để ẩn/hiện cho gọn).
export const ROLE_GROUPS: Record<"all" | "memberManagers", AccountRole[]> = {
  all: ["MEMBER", "HR", "PRESIDENT"],
  memberManagers: ["HR", "PRESIDENT"], // GET /api/members: hasAnyRole('HR', 'PRESIDENT')
};
