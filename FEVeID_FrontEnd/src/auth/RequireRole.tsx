import { Button, Result } from "antd";
import { Link, Outlet } from "react-router";
import type { AccountRole } from "../api/types";
import { useCurrentUser } from "./useMe";

type RequireRoleProps = {
  roles: AccountRole[];
};

// Giống @PreAuthorize("hasAnyRole(...)") nhưng cho TRANG: role không nằm trong danh sách → trang 403.
// Luôn đặt BÊN TRONG RequireAuth (cần biết người dùng là ai trước đã).
// Đây chỉ là trải nghiệm: gõ thẳng URL vẫn bị chặn ở đây, còn dữ liệu thật thì backend đã chặn bằng 403.
export function RequireRole({ roles }: RequireRoleProps) {
  const me = useCurrentUser();

  if (!roles.includes(me.role)) {
    return (
      <Result
        status="403"
        title="403"
        subTitle="Bạn không có quyền xem trang này."
        extra={
          <Link to="/app">
            <Button type="primary">Về trang tổng quan</Button>
          </Link>
        }
      />
    );
  }

  return <Outlet />;
}
