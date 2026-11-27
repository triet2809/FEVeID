import { Card, Flex, Statistic } from "antd";
import type { PointSummaryResponse } from "../api/types";

// Hai con số chính (requirements 11.3). Không có kỳ hiện tại (khoảng trống giữa 2 kỳ) thì nói rõ thay vì hiện 0
export function PointSummaryCards({
  summary,
}: {
  summary: PointSummaryResponse;
}) {
  return (
    <Flex gap={16} wrap>
      <Card style={{ minWidth: 220 }}>
        <Statistic
          title={
            summary.currentTermName
              ? `Số dư kỳ ${summary.currentTermName}`
              : "Hiện không trong kỳ hoạt động"
          }
          value={summary.currentTermName ? summary.currentBalance : "—"}
          suffix={summary.currentTermName ? "điểm" : undefined}
        />
      </Card>
      <Card style={{ minWidth: 220 }}>
        <Statistic
          title="Tổng điểm trọn đời"
          value={summary.lifetimePoints}
          suffix="điểm"
        />
      </Card>
    </Flex>
  );
}
