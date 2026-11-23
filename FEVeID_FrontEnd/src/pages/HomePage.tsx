import { Typography } from "antd";

// Tạm thời: trang chủ công khai. Landing page thật (ảnh lớn, hiệu ứng cuộn) làm ở Bài 14.5.
export function HomePage() {
  return (
    <div className="page">
      <Typography.Title>FEVeID</Typography.Title>
      <Typography.Paragraph>Câu lạc bộ tổ chức sự kiện.</Typography.Paragraph>
    </div>
  );
}
