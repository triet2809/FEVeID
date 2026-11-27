import { Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import type { PointTransactionResponse } from "../api/types";
import { formatDateTime } from "../lib/time";
import { POINT_TYPE_TAGS, signed } from "./pointLabels";

const columns: TableColumnsType<PointTransactionResponse> = [
  {
    title: "Thời gian",
    dataIndex: "createdAt",
    render: (iso: string) => formatDateTime(iso),
  },
  { title: "Kỳ", dataIndex: "termName" },
  {
    title: "Loại",
    dataIndex: "type",
    render: (_, row) => (
      <Tag color={POINT_TYPE_TAGS[row.type].color}>
        {POINT_TYPE_TAGS[row.type].label}
      </Tag>
    ),
  },
  {
    title: "Điểm",
    dataIndex: "amount",
    align: "right",
    render: (amount: number) => (
      <Typography.Text strong type={amount > 0 ? "success" : "danger"}>
        {signed(amount)}
      </Typography.Text>
    ),
  },
  {
    // Điểm từ sự kiện: tên sự kiện; điều chỉnh tay: lý do HR ghi
    title: "Nội dung",
    key: "source",
    render: (_, row) => row.eventName ?? row.reason ?? "—",
  },
  {
    title: "Người ghi",
    dataIndex: "createdByName",
    render: (name?: string) => name ?? "Hệ thống",
  },
];

// Lịch sử cộng/trừ điểm, mới nhất trước (backend đã sắp xếp). Dùng chung cho "Điểm của tôi" và trang HR
export function PointHistoryTable({
  history,
  loading,
}: {
  history: PointTransactionResponse[];
  loading?: boolean;
}) {
  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={history}
      loading={loading}
      pagination={{ pageSize: 20, hideOnSinglePage: true }}
      scroll={{ x: true }}
      locale={{ emptyText: "Chưa có giao dịch điểm nào" }}
    />
  );
}
