import { Alert, Button, Flex, Select, Spin, Typography } from "antd";
import { useState } from "react";
import { useSearchParams } from "react-router";
import { errorMessage } from "../api/client";
import { useMembers } from "../events/eventQueries";
import { AdjustPointsModal } from "../points/AdjustPointsModal";
import { PointHistoryTable } from "../points/PointHistoryTable";
import { PointSummaryCards } from "../points/PointSummaryCards";
import { useMemberPoints } from "../points/pointQueries";

// /app/member-points?member=12 (HR/BCN): chọn thành viên → xem điểm → điều chỉnh tay.
// Thành viên đang chọn nằm trên URL (?member=) nên tải lại trang hoặc gửi link cho người khác vẫn đúng người
export function MemberPointsPage() {
  const [params, setParams] = useSearchParams();
  const memberId = params.get("member")
    ? Number(params.get("member"))
    : undefined;
  const members = useMembers();
  const points = useMemberPoints(memberId);
  const [adjusting, setAdjusting] = useState(false);

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={3} style={{ margin: 0 }}>
        Điểm thành viên
      </Typography.Title>

      <Flex gap={12} wrap align="center">
        {/* showSearch + optionFilterProp: gõ tên hoặc email để lọc, danh sách tải 1 lần từ GET /api/members */}
        <Select
          showSearch
          allowClear
          placeholder="Chọn thành viên (gõ tên hoặc email)"
          style={{ minWidth: 320 }}
          loading={members.isPending}
          value={memberId}
          optionFilterProp="label"
          options={members.data?.map((m) => ({
            value: m.id,
            label: `${m.fullName} · ${m.email}`,
          }))}
          onChange={(id?: number) =>
            setParams(id === undefined ? {} : { member: String(id) })
          }
        />
        {points.isSuccess && (
          <Button type="primary" onClick={() => setAdjusting(true)}>
            Điều chỉnh điểm
          </Button>
        )}
      </Flex>

      {members.isError && (
        <Alert type="error" title={errorMessage(members.error)} />
      )}
      {memberId === undefined && (
        <Typography.Text type="secondary">
          Chọn một thành viên để xem số dư và lịch sử điểm.
        </Typography.Text>
      )}
      {memberId !== undefined && points.isPending && <Spin />}
      {points.isError && (
        <Alert type="error" title={errorMessage(points.error)} />
      )}
      {points.isSuccess && (
        <>
          <PointSummaryCards summary={points.data} />
          <PointHistoryTable history={points.data.history} />
          <AdjustPointsModal
            memberId={points.data.memberId}
            memberName={points.data.fullName}
            open={adjusting}
            onClose={() => setAdjusting(false)}
          />
        </>
      )}
    </Flex>
  );
}
