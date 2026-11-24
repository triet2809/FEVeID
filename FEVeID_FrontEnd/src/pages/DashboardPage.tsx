import { Typography } from "antd";
import { ROLE_LABELS } from "../auth/roles";
import { useCurrentUser } from "../auth/useMe";

// Tạm thời: lời chào. Dashboard thật (số liệu theo role) làm ở Bài 16.
export function DashboardPage() {
  const me = useCurrentUser();

  return (
    <>
      <Typography.Title level={3}>Xin chào, {me.fullName}</Typography.Title>
      <Typography.Paragraph>
        Bạn đang đăng nhập với vai trò <b>{ROLE_LABELS[me.role]}</b>.
      </Typography.Paragraph>
    </>
  );
}
