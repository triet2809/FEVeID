import { Alert, Flex, Spin, Typography } from "antd";
import { errorMessage } from "../api/client";
import { PointHistoryTable } from "../points/PointHistoryTable";
import { PointSummaryCards } from "../points/PointSummaryCards";
import { useMyPoints } from "../points/pointQueries";

// /app/points: điểm của tôi (requirements 11.3). Điểm từ sự kiện được cộng khi HR chốt sự kiện (Bài 13)
export function MyPointsPage() {
  const points = useMyPoints();

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={3} style={{ margin: 0 }}>
        Điểm của tôi
      </Typography.Title>
      {points.isPending && <Spin />}
      {points.isError && (
        <Alert type="error" title={errorMessage(points.error)} />
      )}
      {points.isSuccess && (
        <>
          <PointSummaryCards summary={points.data} />
          <Typography.Text type="secondary">
            Tổ chức +4, hỗ trợ +2, tham gia (có check-in) +1. Điểm được cộng khi
            sự kiện được chốt; sang kỳ mới số dư về 0, tổng trọn đời vẫn giữ.
          </Typography.Text>
          <PointHistoryTable history={points.data.history} />
        </>
      )}
    </Flex>
  );
}
