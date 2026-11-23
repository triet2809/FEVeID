import { Descriptions, Typography } from "antd";
import type { MemberResponse } from "../api/types";
import { ROLE_LABELS } from "../auth/roles";
import { useCurrentUser } from "../auth/useMe";

// Record<enum, string>: thiếu một trạng thái là TS báo lỗi ngay (giống switch đủ case trên enum Java)
const STATUS_LABELS: Record<MemberResponse["memberStatus"], string> = {
  CTV: "Cộng tác viên",
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Tạm nghỉ",
  DISQUALIFIED: "Đã loại",
};

// Dữ liệu lấy từ cache /me (đã tải lúc vào khu nội bộ) → không gọi thêm API nào
export function ProfilePage() {
  const me = useCurrentUser();

  return (
    <>
      <Typography.Title level={3}>Hồ sơ của tôi</Typography.Title>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="Họ tên">{me.fullName}</Descriptions.Item>
        <Descriptions.Item label="Email">{me.email}</Descriptions.Item>
        {/* phone, studentCode có thể null → "??" lấy giá trị bên phải khi bên trái null/undefined */}
        <Descriptions.Item label="MSSV">{me.studentCode ?? "—"}</Descriptions.Item>
        <Descriptions.Item label="Số điện thoại">{me.phone ?? "—"}</Descriptions.Item>
        <Descriptions.Item label="Vai trò">{ROLE_LABELS[me.role]}</Descriptions.Item>
        <Descriptions.Item label="Trạng thái">{STATUS_LABELS[me.memberStatus]}</Descriptions.Item>
        <Descriptions.Item label="Ngày tham gia">{me.joinedAt}</Descriptions.Item>
      </Descriptions>
    </>
  );
}
