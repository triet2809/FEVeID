import {
  CalendarOutlined,
  CarryOutOutlined,
  DashboardOutlined,
  GlobalOutlined,
  LogoutOutlined,
  ScheduleOutlined,
  StarOutlined,
  TeamOutlined,
  TrophyOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Button, Layout, Menu, Space, Tag, Typography } from "antd";
import type { ReactNode } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import type { AccountRole } from "../api/types";
import { ROLE_GROUPS, ROLE_LABELS } from "../auth/roles";
import { useLogout } from "../auth/useLogout";
import { useCurrentUser } from "../auth/useMe";

type MenuEntry = {
  path: string;
  label: string;
  icon: ReactNode;
  roles: AccountRole[]; // role nào thấy mục này
};

// Danh sách mục menu của khu nội bộ. Thêm trang mới: thêm 1 dòng ở đây + 1 <Route> trong App.tsx
// (dùng CÙNG nhóm role trong ROLE_GROUPS để menu và chặn URL luôn khớp nhau).
const MENU: MenuEntry[] = [
  { path: "/app", label: "Tổng quan", icon: <DashboardOutlined />, roles: ROLE_GROUPS.all },
  { path: "/app/profile", label: "Hồ sơ của tôi", icon: <UserOutlined />, roles: ROLE_GROUPS.all },
  { path: "/app/my-events", label: "Sự kiện của tôi", icon: <ScheduleOutlined />, roles: ROLE_GROUPS.all },
  { path: "/app/points", label: "Điểm của tôi", icon: <StarOutlined />, roles: ROLE_GROUPS.all },
  // Ra khu công khai (PublicLayout, có nút "Khu thành viên" để quay lại)
  { path: "/events", label: "Sự kiện CLB", icon: <GlobalOutlined />, roles: ROLE_GROUPS.all },
  { path: "/app/members", label: "Thành viên", icon: <TeamOutlined />, roles: ROLE_GROUPS.memberManagers },
  { path: "/app/terms", label: "Kỳ học", icon: <CalendarOutlined />, roles: ROLE_GROUPS.memberManagers },
  { path: "/app/member-points", label: "Điểm thành viên", icon: <TrophyOutlined />, roles: ROLE_GROUPS.memberManagers },
  { path: "/app/events", label: "Sự kiện", icon: <CarryOutOutlined />, roles: ROLE_GROUPS.eventManagers },
];

// Mục menu ứng với URL hiện tại: /app/events/12 vẫn tô đậm "Sự kiện".
// Lấy mục có path DÀI NHẤT là tiền tố của URL (nếu không, "/app" sẽ khớp với mọi trang)
function selectedMenuKey(pathname: string): string {
  const matches = MENU.filter((entry) => pathname === entry.path || pathname.startsWith(`${entry.path}/`));
  return matches.reduce((best, entry) => (entry.path.length > best.length ? entry.path : best), "/app");
}

// Khung chung cho mọi trang đã đăng nhập: menu bên trái, thanh trên (tên + role + đăng xuất), nội dung ở giữa
export function AppLayout() {
  const me = useCurrentUser();
  const logout = useLogout();
  const navigate = useNavigate();
  const location = useLocation();

  // Lọc menu theo role rồi đổi sang dạng antd Menu cần ({ key, icon, label })
  const items = MENU.filter((entry) => entry.roles.includes(me.role)).map((entry) => ({
    key: entry.path,
    icon: entry.icon,
    label: entry.label,
  }));

  return (
    <Layout style={{ minHeight: "100vh" }}>
      {/* breakpoint="lg": màn hình nhỏ hơn 992px (điện thoại) thì menu tự thu lại, có nút mở */}
      <Layout.Sider theme="light" breakpoint="lg" collapsedWidth={0}>
        <Typography.Title level={4} style={{ padding: "16px 24px", margin: 0 }}>
          FEVeID
        </Typography.Title>
        <Menu
          mode="inline"
          items={items}
          selectedKeys={[selectedMenuKey(location.pathname)]} // tô đậm mục đúng với URL hiện tại
          onClick={({ key }) => navigate(key)} // key chính là path
        />
      </Layout.Sider>

      <Layout>
        <Layout.Header
          style={{
            display: "flex",
            justifyContent: "flex-end",
            alignItems: "center",
            borderBottom: "1px solid #e5e7eb",
          }}
        >
          <Space>
            <Typography.Text strong>{me.fullName}</Typography.Text>
            <Tag color="purple">{ROLE_LABELS[me.role]}</Tag>
            <Button icon={<LogoutOutlined />} onClick={logout}>
              Đăng xuất
            </Button>
          </Space>
        </Layout.Header>

        <Layout.Content>
          <div className="page">
            {/* Outlet = chỗ trống để trang con (Dashboard, Profile...) hiện vào */}
            <Outlet />
          </div>
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
