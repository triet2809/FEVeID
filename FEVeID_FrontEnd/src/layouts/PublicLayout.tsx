import { Button, Flex, Layout, Typography } from "antd";
import { Link, Outlet, useLocation } from "react-router";
import { useMe } from "../auth/useMe";

// Khung cho khu công khai (khách + member). Không có RequireAuth: khách vào được.
// Bản tạm gọn theo Ant Design; landing page nhiều hình ảnh + hiệu ứng làm ở Bài 14.5
export function PublicLayout() {
  const me = useMe(); // khách → 401 → isError; không chuyển trang như RequireAuth, chỉ để đổi nút góc phải
  const location = useLocation();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Layout.Header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 16,
          background: "#fff",
          borderBottom: "1px solid #e5e7eb",
          paddingInline: 16,
        }}
      >
        <Flex align="center" gap={24}>
          <Link to="/">
            <Typography.Title level={4} style={{ margin: 0 }}>
              FEVeID
            </Typography.Title>
          </Link>
          <Link to="/events">Sự kiện</Link>
        </Flex>

        {me.isSuccess ? (
          <Link to="/app">
            <Button>Khu thành viên</Button>
          </Link>
        ) : (
          // Đăng nhập xong quay lại đúng trang đang xem (LoginPage đọc state.from)
          <Link to="/login" state={{ from: location.pathname }}>
            <Button type="primary">Đăng nhập</Button>
          </Link>
        )}
      </Layout.Header>

      <Layout.Content>
        <div className="page">
          <Outlet />
        </div>
      </Layout.Content>
    </Layout>
  );
}
