import dayjs, { type Dayjs } from "dayjs";
import timezone from "dayjs/plugin/timezone";
import utc from "dayjs/plugin/utc";

// dayjs ≈ java.time của frontend (antd DatePicker cũng dùng dayjs). Bật thêm plugin múi giờ
dayjs.extend(utc);
dayjs.extend(timezone);

// Giống TimeConfig.CLUB_ZONE bên backend
export const CLUB_TZ = "Asia/Ho_Chi_Minh";

// Backend trả Instant dạng UTC: "2026-12-05T01:00:00Z" → hiển thị "05/12/2026 08:00" (giờ VN)
export function formatDateTime(iso: string | undefined): string {
  return iso ? dayjs(iso).tz(CLUB_TZ).format("DD/MM/YYYY HH:mm") : "—";
}

// Ô chọn giờ (DatePicker) dùng giờ của máy người dùng. Thành viên CLB ở VN nên giờ máy = giờ VN.
// Đổi qua lại: chuỗi ISO từ backend ↔ Dayjs trong form
export function toPickerValue(iso: string | undefined): Dayjs | undefined {
  return iso ? dayjs(iso) : undefined;
}

// Dayjs → "2026-12-05T01:00:00.000Z": cùng một mốc thời gian, backend nhận thẳng vào Instant
export function toIso(value: Dayjs | null | undefined): string | undefined {
  return value ? value.toISOString() : undefined;
}

export function isFuture(iso: string): boolean {
  return dayjs(iso).isAfter(dayjs());
}
