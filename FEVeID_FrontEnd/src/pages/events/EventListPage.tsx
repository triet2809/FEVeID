import { PlusOutlined } from "@ant-design/icons";
import { Alert, Button, Flex, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { Link, useNavigate } from "react-router";
import { errorMessage } from "../../api/client";
import type { EventResponse } from "../../api/types";
import {
  phaseNameToShow,
  STATUS_TAGS,
  VISIBILITY_LABELS,
} from "../../events/eventLabels";
import { useEvents } from "../../events/eventQueries";
import { formatDateTime } from "../../lib/time";

// Khai báo cột (giống chọn field nào của DTO để hiện). render = cách vẽ ô từ một dòng dữ liệu
const columns: TableColumnsType<EventResponse> = [
  {
    title: "Tên sự kiện",
    dataIndex: "name",
    render: (name: string, event) => (
      <Link to={`/app/events/${event.id}`}>{name}</Link>
    ),
  },
  {
    title: "Trạng thái",
    dataIndex: "status",
    render: (_, event) => (
      <Flex vertical gap={4} align="flex-start">
        <Tag color={STATUS_TAGS[event.status].color}>
          {STATUS_TAGS[event.status].label}
        </Tag>
        {/* Tên giai đoạn tùy chỉnh, vd "Check-in", "Biểu diễn" (chỉ có khi đã công bố) */}
        {phaseNameToShow(event) && (
          <Typography.Text type="secondary">
            {phaseNameToShow(event)}
          </Typography.Text>
        )}
      </Flex>
    ),
  },
  {
    title: "Bắt đầu",
    dataIndex: "startsAt",
    render: (iso?: string) => formatDateTime(iso),
  },
  {
    title: "Kết thúc",
    dataIndex: "endsAt",
    render: (iso?: string) => formatDateTime(iso),
  },
  {
    title: "Đối tượng",
    dataIndex: "visibility",
    render: (_, event) => VISIBILITY_LABELS[event.visibility],
  },
  {
    title: "Phụ trách",
    dataIndex: ["owner", "fullName"],
    render: (name?: string) => name ?? "—",
  },
];

export function EventListPage() {
  const navigate = useNavigate();
  const events = useEvents();

  return (
    <>
      <Flex
        justify="space-between"
        align="center"
        wrap
        gap={8}
        style={{ marginBottom: 16 }}
      >
        <Typography.Title level={3} style={{ margin: 0 }}>
          Sự kiện
        </Typography.Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate("/app/events/new")}
        >
          Tạo sự kiện
        </Button>
      </Flex>

      {events.isError && (
        <Alert
          type="error"
          title={errorMessage(events.error)}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* rowKey: mỗi dòng cần một khóa duy nhất (giống @Id) để React biết dòng nào đổi.
          scroll x: màn hình hẹp (điện thoại) thì cuộn ngang trong bảng thay vì vỡ trang */}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={events.data}
        loading={events.isPending}
        pagination={{ pageSize: 20, hideOnSinglePage: true }}
        scroll={{ x: 800 }}
      />
    </>
  );
}
