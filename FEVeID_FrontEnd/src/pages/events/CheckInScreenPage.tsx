import { Flex, QRCode, Result, Spin, Typography } from "antd";
import { Link, useParams } from "react-router";
import { errorMessage, HttpError } from "../../api/client";
import { useEvent } from "../../events/eventQueries";
import { useCheckInCode } from "../../registrations/registrationQueries";

// Link nằm trong QR: điện thoại quét bằng camera mặc định là mở thẳng trang /app/check-in (CheckInPage),
// không cần app hay thư viện quét QR. Mã nằm trong link nên link cũng chỉ sống 30–60 giây như mã
function checkInLink(eventId: number, code: string): string {
  const params = new URLSearchParams({ event: String(eventId), code });
  return `${window.location.origin}/app/check-in?${params}`;
}

// /app/events/:id/check-in-screen: chiếu lên màn hình lớn tại sự kiện (HR/BCN). Không nằm trong AppLayout
// (không có menu) để QR to hết cỡ. Mã tự đổi theo useCheckInCode, trang không cần bấm gì
export function CheckInScreenPage() {
  const id = Number(useParams().id);
  const event = useEvent(id);
  const code = useCheckInCode(id);

  const backLink = (
    <Link to={`/app/events/${id}`}>Về trang quản lý sự kiện</Link>
  );

  if (code.isPending) {
    return <Spin size="large" fullscreen />;
  }
  if (code.isError) {
    // 409: chưa tới / đã qua giai đoạn ONGOING. Trang vẫn tự hỏi lại mỗi 5 giây, tới giờ là hiện mã
    const notOngoing =
      code.error instanceof HttpError && code.error.status === 409;
    return (
      <Result
        status={notOngoing ? "info" : "error"}
        title={event.data?.name}
        subTitle={
          notOngoing
            ? "Chưa tới giờ check-in hoặc sự kiện đã kết thúc. Tới giờ, mã QR sẽ tự hiện ở đây."
            : errorMessage(code.error)
        }
        extra={backLink}
      />
    );
  }

  const { code: current, checkedInCount, expectedCount } = code.data;
  return (
    <Flex
      vertical
      align="center"
      justify="center"
      gap={24}
      style={{ minHeight: "100vh", padding: 24 }}
    >
      <Typography.Title level={2} style={{ margin: 0, textAlign: "center" }}>
        {event.data?.name ?? "Check-in"}
      </Typography.Title>
      <Typography.Text style={{ fontSize: 20 }}>
        Mở camera điện thoại, quét mã để check-in
      </Typography.Text>
      {/* Kích thước theo màn hình: cạnh ngắn hơn của khung nhìn, chừa chỗ cho chữ */}
      <QRCode
        value={checkInLink(id, current)}
        size={Math.min(window.innerWidth - 48, window.innerHeight - 260, 640)}
        errorLevel="M"
        bordered={false}
      />
      <Typography.Text style={{ fontSize: 28 }}>
        Đã check-in: <strong>{checkedInCount}</strong> / {expectedCount}
      </Typography.Text>
      <Typography.Text type="secondary">
        Mã tự đổi mỗi 30 giây · {backLink}
      </Typography.Text>
    </Flex>
  );
}
