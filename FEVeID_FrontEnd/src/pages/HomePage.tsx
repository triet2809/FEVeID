import { Button, Space, Typography } from "antd";
import { Link } from "react-router";

// Tạm thời: trang chủ công khai. Landing page thật (ảnh lớn, hiệu ứng cuộn) làm ở Bài 14.5.
export function HomePage() {
  return (
    <div className="page">
      <Typography.Title>FEVeID</Typography.Title>
      <Typography.Paragraph>Câu lạc bộ tổ chức sự kiện.</Typography.Paragraph>
      {/* Đã đăng nhập thì /login tự chuyển vào /app, nên một nút là đủ */}
      <Space>
        <Link to="/events">
          <Button type="primary">Xem sự kiện</Button>
        </Link>
        <Link to="/app">
          <Button>Vào khu thành viên</Button>
        </Link>
      </Space>
    </div>
  );
}
