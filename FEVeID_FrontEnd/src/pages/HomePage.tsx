import { Button, Typography } from "antd";
import { Link } from "react-router";

// Tạm thời: trang chủ công khai. Landing page thật (ảnh lớn, hiệu ứng cuộn) làm ở Bài 14.5.
export function HomePage() {
  return (
    <div className="page">
      <Typography.Title>FEVeID</Typography.Title>
      <Typography.Paragraph>Câu lạc bộ tổ chức sự kiện.</Typography.Paragraph>
      {/* Đã đăng nhập thì /login tự chuyển vào /app, nên một nút là đủ */}
      <Link to="/app">
        <Button type="primary">Vào khu thành viên</Button>
      </Link>
    </div>
  );
}
