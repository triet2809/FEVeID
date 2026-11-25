import {
  Alert,
  Button,
  Empty,
  Flex,
  Popconfirm,
  Table,
  Tag,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { Link } from "react-router";
import { errorMessage } from "../api/client";
import type { MyRegistrationResponse } from "../api/types";
import { STATUS_TAGS } from "../events/eventLabels";
import { formatDateTime } from "../lib/time";
import { REGISTRATION_STATUS_TAGS } from "../registrations/registrationLabels";
import {
  useCancelRegistration,
  useMyRegistrations,
} from "../registrations/registrationQueries";

// /app/my-events: các sự kiện mình đã đăng ký (requirements 2.4). Wishlist "quan tâm" làm sau
export function MyEventsPage() {
  const mine = useMyRegistrations();
  const cancel = useCancelRegistration();

  const columns: TableColumnsType<MyRegistrationResponse> = [
    {
      title: "Sự kiện",
      dataIndex: ["event", "name"],
      // Mở trang công khai của sự kiện (member xem được cả sự kiện nội bộ)
      render: (name: string, row) => (
        <Link to={`/events/${row.event.id}`}>{name}</Link>
      ),
    },
    {
      title: "Thời gian",
      dataIndex: ["event", "startsAt"],
      render: (iso: string) => formatDateTime(iso),
    },
    {
      title: "Trạng thái sự kiện",
      key: "eventStatus",
      render: (_, row) => (
        <Tag color={STATUS_TAGS[row.event.status].color}>
          {STATUS_TAGS[row.event.status].label}
        </Tag>
      ),
    },
    {
      title: "Đăng ký",
      dataIndex: "status",
      render: (_, row) => (
        <Tag color={REGISTRATION_STATUS_TAGS[row.status].color}>
          {REGISTRATION_STATUS_TAGS[row.status].label}
        </Tag>
      ),
    },
    {
      title: "",
      key: "actions",
      render: (_, row) =>
        row.cancellable && (
          <Popconfirm
            title="Hủy đăng ký?"
            okText="Hủy đăng ký"
            okButtonProps={{ danger: true }}
            cancelText="Không"
            onConfirm={() => cancel.mutate(row.id)}
          >
            {/* Chỉ quay ở đúng dòng đang hủy (variables = id vừa gửi đi) */}
            <Button
              danger
              size="small"
              loading={cancel.isPending && cancel.variables === row.id}
            >
              Hủy
            </Button>
          </Popconfirm>
        ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={3} style={{ margin: 0 }}>
        Sự kiện của tôi
      </Typography.Title>

      {mine.isError ? (
        <Alert type="error" title={errorMessage(mine.error)} />
      ) : (
        <Table
          rowKey="id"
          columns={columns}
          dataSource={mine.data}
          loading={mine.isPending}
          pagination={false}
          scroll={{ x: true }} // điện thoại: bảng cuộn ngang thay vì bóp méo cột
          locale={{
            emptyText: (
              <Empty description="Bạn chưa đăng ký sự kiện nào">
                <Link to="/events">Xem các sự kiện</Link>
              </Empty>
            ),
          }}
        />
      )}
    </Flex>
  );
}
