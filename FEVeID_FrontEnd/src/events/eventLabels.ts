import type {
  EventResponse,
  EventStatus,
  EventVisibility,
  PhaseKind,
} from "../api/types";

// Record<enum, ...>: backend thêm trạng thái mới → sinh lại schema.d.ts → TS báo thiếu ở đây
export const STATUS_TAGS: Record<
  EventStatus,
  { label: string; color: string }
> = {
  DRAFT: { label: "Bản nháp", color: "default" },
  UPCOMING: { label: "Sắp diễn ra", color: "blue" },
  ONGOING: { label: "Đang diễn ra", color: "green" },
  COMPLETED: { label: "Đã kết thúc", color: "purple" },
  CANCELLED: { label: "Đã hủy", color: "red" },
};

export const VISIBILITY_LABELS: Record<EventVisibility, string> = {
  INTERNAL: "Nội bộ",
  PUBLIC: "Công khai",
};

export const PHASE_KIND_LABELS: Record<PhaseKind, string> = {
  UPCOMING: "Sắp diễn ra",
  ONGOING: "Đang diễn ra",
  COMPLETED: "Kết thúc",
};

// Tên giai đoạn tùy chỉnh ("Check-in", "Biểu diễn"). Ẩn khi trùng nhãn trạng thái để khỏi hiện 2 lần "Sắp diễn ra"
// Nhận cả EventResponse (trang quản lý) lẫn PublicEventResponse (trang công khai): chỉ cần 2 field này
export function phaseNameToShow(event: {
  status: EventStatus;
  currentPhaseName?: string;
}): string | undefined {
  const name = event.currentPhaseName;
  return name && name !== STATUS_TAGS[event.status].label ? name : undefined;
}

// Giống ensureScheduleEditable bên backend: đã kết thúc/đã hủy thì khóa mọi chỉnh sửa.
// Frontend chỉ dùng để làm mờ nút cho dễ hiểu; backend vẫn là nơi chặn thật (409)
export function isLocked(event: EventResponse): boolean {
  return event.status === "COMPLETED" || event.status === "CANCELLED";
}

// Cloudinary đổi kích thước ngay trên URL: chèn "w_800,c_limit,f_auto,q_auto" sau "/upload/"
//   w_800   : rộng tối đa 800px (ảnh gốc 4000px, 8MB → vài trăm KB)
//   c_limit : chỉ thu nhỏ, không phóng to ảnh nhỏ
//   f_auto  : tự chọn định dạng nhẹ nhất trình duyệt hỗ trợ (WebP/AVIF)
//   q_auto  : tự chọn mức nén vừa đủ đẹp
export function bannerThumb(url: string, width: number): string {
  return url.replace("/upload/", `/upload/w_${width},c_limit,f_auto,q_auto/`);
}
