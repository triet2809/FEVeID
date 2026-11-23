import { useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { useNavigate } from "react-router";
import { logout } from "./authApi";

// Đăng xuất = nhờ backend xóa cookie + XÓA CACHE dữ liệu + về /login.
// Quên xóa cache thì người đăng nhập sau (cùng máy) sẽ thấy dữ liệu của người trước.
export function useLogout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useCallback(async () => {
    try {
      await logout();
    } catch {
      // Mất mạng/backend sập: vẫn dọn phía trình duyệt cho người dùng thấy đã thoát.
      // (Cookie có thể còn lại tới khi hết hạn — chấp nhận được, lần sau bấm lại là xong.)
    }
    queryClient.clear();
    navigate("/login", { replace: true });
  }, [navigate, queryClient]);
}
