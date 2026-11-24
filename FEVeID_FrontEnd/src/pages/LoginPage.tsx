import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button, Card, Form, Input, Spin, Typography } from "antd";
import { Navigate, useLocation } from "react-router";
import { errorMessage } from "../api/client";
import type { LoginRequest } from "../api/types";
import { login } from "../auth/authApi";
import { ME_QUERY_KEY, useMe } from "../auth/useMe";

export function LoginPage() {
  const location = useLocation();
  const queryClient = useQueryClient();
  const me = useMe(); // trang này cũng hỏi /me: chưa đăng nhập thì 401 (bình thường, Console sẽ thấy 1 dòng đỏ)

  // RequireAuth gửi kèm trang người dùng định vào (state.from); không có thì vào trang tổng quan
  const from = (location.state as { from?: string } | null)?.from ?? "/app";

  const loginMutation = useMutation({
    mutationFn: login,
    // Login thành công thì cookie đã có, nhưng cache vẫn đang nhớ "/me = 401".
    // invalidate = đánh dấu cache cũ là hết hạn → tự gọi lại /me → lần này 200 → me.isSuccess
    // → dòng `if (me.isSuccess)` bên dưới tự chuyển trang. Không cần gọi navigate() ở đây.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY }),
  });

  // Đã đăng nhập (vừa login xong, HOẶC đang đăng nhập mà gõ /login) → không cho ở lại trang này
  if (me.isSuccess) {
    return <Navigate to={from} replace />;
  }
  // Lần đầu mở trang, đang chờ /me → chưa biết đã đăng nhập chưa, đừng vội hiện form
  if (me.isPending) {
    return <Spin size="large" fullscreen />;
  }

  return (
    <Card style={{ maxWidth: 400, margin: "64px auto" }}>
      <Typography.Title level={3}>Đăng nhập FEVeID</Typography.Title>

      {/* Form của antd tự giữ giá trị và tự kiểm tra; onFinish chỉ chạy khi mọi ô hợp lệ */}
      <Form<LoginRequest>
        layout="vertical"
        requiredMark={false}
        onFinish={(values) => loginMutation.mutate(values)}
      >
        <Form.Item
          label="Email"
          name="email"
          rules={[
            { required: true, message: "Hãy nhập email" },
            { type: "email", message: "Email không đúng định dạng" },
          ]}
        >
          <Input autoComplete="email" />
        </Form.Item>

        <Form.Item
          label="Mật khẩu"
          name="password"
          rules={[{ required: true, message: "Hãy nhập mật khẩu" }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>

        {/* Lỗi từ backend: "Email hoặc mật khẩu không đúng", "Tài khoản bị khóa..." (GlobalExceptionHandler) */}
        {loginMutation.isError && (
          <Typography.Paragraph type="danger">
            {errorMessage(loginMutation.error)}
          </Typography.Paragraph>
        )}

        {/* isPending: đang chờ login HOẶC đang tải lại /me → khóa nút để không bấm 2 lần */}
        <Button
          type="primary"
          htmlType="submit"
          loading={loginMutation.isPending}
          block
        >
          Đăng nhập
        </Button>
      </Form>
    </Card>
  );
}
