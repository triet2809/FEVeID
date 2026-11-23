import { Result, Spin } from "antd";
import { Navigate, Outlet, useLocation } from "react-router";
import { errorMessage, isUnauthorized } from "../api/client";
import { useMe } from "./useMe";

// "Người gác cổng" cho khu nội bộ (giống Security filter): chưa đăng nhập → về /login.
// Khác playground: không còn token trong localStorage để kiểm tra, nên phải HỎI backend (useMe).
export function RequireAuth() {
  const location = useLocation();
  const me = useMe();

  // 1. Đang chờ /me trả lời → chưa biết là ai, hiện vòng xoay (không được vội đá về /login)
  if (me.isPending) {
    return <Spin size="large" fullscreen />;
  }

  if (me.isError) {
    // 2. 401 → chưa đăng nhập / cookie hết hạn. Ghi nhớ trang đang muốn vào (state.from)
    //    để đăng nhập xong quay lại đúng trang đó.
    if (isUnauthorized(me.error)) {
      return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    }
    // 3. Lỗi khác (mất mạng, backend sập) → báo lỗi, KHÔNG đá về /login (đăng nhập lại cũng vô ích)
    return (
      <Result
        status="error"
        title="Không tải được thông tin tài khoản"
        subTitle={errorMessage(me.error)}
      />
    );
  }

  // 4. Đã đăng nhập → vẽ trang con
  return <Outlet />;
}
