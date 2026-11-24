import { useQuery } from "@tanstack/react-query";
import type { MemberResponse } from "../api/types";
import { fetchMe } from "./authApi";

export const ME_QUERY_KEY = ["me"];

// "Phiên đăng nhập" của cả app = kết quả GET /api/members/me, lưu trong cache TanStack Query.
// Mọi component gọi useMe() dùng chung MỘT kết quả (chỉ 1 request), giống một bean dùng chung bên Spring.
//   - đang tải  → isPending
//   - 401       → isError (chưa đăng nhập / cookie hết hạn)
//   - 200       → data = thông tin người đang đăng nhập (có role)
export function useMe() {
  return useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: fetchMe,
  });
}

// Dùng trong các trang nằm BÊN TRONG RequireAuth: tới được đó thì chắc chắn đã có data,
// nên trả thẳng MemberResponse (không phải kiểm tra undefined ở từng trang).
export function useCurrentUser(): MemberResponse {
  const { data } = useMe();
  if (data === undefined) {
    // Chỉ xảy ra khi lỡ dùng hook này ở trang công khai → báo lỗi rõ ràng thay vì lỗi khó hiểu ở chỗ khác
    throw new Error("useCurrentUser() chỉ dùng được bên trong <RequireAuth>");
  }
  return data;
}
