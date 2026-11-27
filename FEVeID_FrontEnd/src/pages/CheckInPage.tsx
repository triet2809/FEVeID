import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button, Result, Spin } from "antd";
import { useEffect, useRef } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router";
import { errorMessage } from "../api/client";
import type { CheckInResponse } from "../api/types";
import { formatDateTime } from "../lib/time";
import { checkIn } from "../registrations/registrationApi";
import { MY_REGISTRATIONS_KEY } from "../registrations/registrationQueries";

// /app/check-in?event=12&code=kR3x...: trang điện thoại mở ra khi quét QR ở màn hình chiếu (CheckInScreenPage).
// Mở trang là tự gửi check-in, không bắt bấm thêm nút. Chưa đăng nhập thì RequireAuth đưa về /login
// rồi quay lại đúng link này (kể cả ?event=&code=)
export function CheckInPage() {
  const [params] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Kết quả lần check-in trước nằm trong state của lịch sử trình duyệt (xem onSuccess)
  const done = (location.state as { checkedIn?: CheckInResponse } | null)
    ?.checkedIn;
  const eventId = Number(params.get("event"));
  const code = params.get("code");

  const mutation = useMutation({
    mutationFn: () => checkIn(eventId, code!),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: MY_REGISTRATIONS_KEY });
      // Xóa mã khỏi URL, giữ kết quả trong state (giống "redirect sau POST" bên server):
      // tải lại trang sau 1 phút sẽ hiện lại "thành công" thay vì gửi mã đã hết hạn lên rồi báo lỗi
      navigate(location.pathname, {
        replace: true,
        state: { checkedIn: result },
      });
    },
  });

  // Chỉ gửi đúng 1 lần. StrictMode (lúc dev) chạy useEffect 2 lần; backend vẫn chịu được
  // (quét lần 2 trả 200), nhưng không cần gửi thừa
  const sent = useRef(false);
  const valid = Number.isInteger(eventId) && eventId > 0 && !!code;
  useEffect(() => {
    if (!done && valid && !sent.current) {
      sent.current = true;
      mutation.mutate();
    }
  }, [done, valid, mutation]);

  const toMyEvents = (
    <Link to="/app/my-events">
      <Button>Sự kiện của tôi</Button>
    </Link>
  );

  if (done) {
    return (
      <Result
        status="success"
        title="Check-in thành công"
        subTitle={`${done.eventName} · lúc ${formatDateTime(done.checkedInAt)}`}
        extra={toMyEvents}
      />
    );
  }
  if (!valid) {
    return (
      <Result
        status="warning"
        title="Link check-in không hợp lệ"
        subTitle="Hãy quét mã QR trên màn hình tại sự kiện bằng camera điện thoại."
        extra={toMyEvents}
      />
    );
  }
  if (mutation.isError) {
    // vd 400 "Mã check-in không đúng hoặc đã hết hạn...", 409 "Sự kiện đã hết chỗ"
    return (
      <Result
        status="error"
        title="Chưa check-in được"
        subTitle={errorMessage(mutation.error)}
        extra={toMyEvents}
      />
    );
  }
  return <Spin size="large" fullscreen description="Đang check-in..." />;
}
