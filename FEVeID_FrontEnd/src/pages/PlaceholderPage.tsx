import { Result } from "antd";

type PlaceholderPageProps = {
  title: string;
};

// Giữ chỗ cho trang chưa làm, để thử menu + phân quyền theo role ngay từ bây giờ
export function PlaceholderPage({ title }: PlaceholderPageProps) {
  return <Result status="info" title={title} subTitle="Trang này sẽ làm ở các bài sau." />;
}
