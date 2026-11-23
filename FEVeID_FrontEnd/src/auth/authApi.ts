import { api } from "../api/client";
import type { LoginRequest, MemberResponse } from "../api/types";

// Thành công → 204, backend gửi Set-Cookie access_token (httpOnly). JS KHÔNG nhận token,
// trình duyệt tự cất và tự gửi kèm các request sau.
export function login(request: LoginRequest): Promise<void> {
  return api.post<void>("/api/auth/login", request);
}

// Backend trả Set-Cookie access_token rỗng + Max-Age=0 → trình duyệt xóa cookie
export function logout(): Promise<void> {
  return api.post<void>("/api/auth/logout");
}

// Frontend không giữ token nên KHÔNG tự biết mình đã đăng nhập chưa:
// hỏi backend "tôi là ai?". 200 = đã đăng nhập (kèm tên, role), 401 = chưa.
export function fetchMe(): Promise<MemberResponse> {
  return api.get<MemberResponse>("/api/members/me");
}
