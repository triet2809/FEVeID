import { QrcodeOutlined } from "@ant-design/icons";
import { Alert, Button, Flex, Input, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useState } from "react";
import { Link } from "react-router";
import { errorMessage } from "../api/client";
import type { EventRegistrationResponse, EventResponse } from "../api/types";
import { formatDateTime } from "../lib/time";
import {
  isActiveRegistration,
  REGISTRATION_STATUS_TAGS,
} from "../registrations/registrationLabels";
import {
  useEventRegistrations,
  useManualCheckIn,
} from "../registrations/registrationQueries";

const baseColumns: TableColumnsType<EventRegistrationResponse> = [
  {
    title: "#",
    key: "index",
    width: 56,
    render: (_, __, index) => index + 1, // thứ tự đăng ký (backend trả người đăng ký trước đứng trước)
  },
  { title: "Họ tên", dataIndex: "fullName" },
  {
    title: "Loại",
    dataIndex: "member",
    render: (member: boolean) =>
      member ? <Tag color="purple">Thành viên</Tag> : <Tag>Khách</Tag>,
  },
  { title: "Email", dataIndex: "email" },
  { title: "SĐT", dataIndex: "phone", render: (v?: string) => v ?? "—" },
  {
    title: "MSSV / Trường",
    dataIndex: "studentCodeOrSchool",
    render: (v?: string) => v ?? "—",
  },
  {
    title: "Trạng thái",
    dataIndex: "status",
    render: (_, row) => (
      <Tag color={REGISTRATION_STATUS_TAGS[row.status].color}>
        {REGISTRATION_STATUS_TAGS[row.status].label}
      </Tag>
    ),
  },
  {
    title: "Đăng ký lúc",
    dataIndex: "createdAt",
    render: (iso: string) => formatDateTime(iso),
  },
];

// Tìm không phân biệt hoa thường trong tên, email, MSSV/trường (Bài 12 dùng để check-in tay)
function matches(row: EventRegistrationResponse, keyword: string): boolean {
  const k = keyword.trim().toLowerCase();
  return [row.fullName, row.email, row.studentCodeOrSchool ?? ""].some((v) =>
    v.toLowerCase().includes(k),
  );
}

// Tab "Đăng ký & check-in" trong trang quản lý sự kiện (HR/BCN).
// Đang diễn ra (ONGOING): danh sách tự tải lại 10 giây/lần, có nút check-in tay và nút mở màn hình QR.
// Đã kết thúc mà chưa chốt: vẫn check-in tay được (check-in bù cho người quên quét mã)
export function RegistrationList({ event }: { event: EventResponse }) {
  const ongoing = event.status === "ONGOING";
  const canCheckInManually =
    ongoing || (event.status === "COMPLETED" && !event.finalizedAt);
  const registrations = useEventRegistrations(event.id, ongoing);
  const manual = useManualCheckIn(event.id);
  const [keyword, setKeyword] = useState("");

  // Cột check-in cần biết sự kiện có đang diễn ra không và nút nào đang chờ → khai báo trong component
  const columns: TableColumnsType<EventRegistrationResponse> = [
    ...baseColumns,
    {
      title: "Check-in",
      key: "checkIn",
      render: (_, row) => {
        if (row.checkedInAt) return formatDateTime(row.checkedInAt);
        // Chỉ đăng ký đã xác nhận mới check-in tay được (khớp checkInManually bên backend)
        if (canCheckInManually && row.status === "CONFIRMED") {
          return (
            <Button
              size="small"
              loading={manual.isPending && manual.variables === row.id}
              onClick={() => manual.mutate(row.id)}
            >
              Check-in
            </Button>
          );
        }
        return "—";
      },
    },
  ];

  if (registrations.isError) {
    return <Alert type="error" title={errorMessage(registrations.error)} />;
  }

  const rows = registrations.data ?? [];
  const active = rows.filter((r) => isActiveRegistration(r.status)).length;
  const checkedIn = rows.filter((r) => r.status === "CHECKED_IN").length;
  const shown = keyword ? rows.filter((r) => matches(r, keyword)) : rows;

  return (
    <Flex vertical gap={12}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Flex gap={16} wrap align="center">
          <Typography.Text>
            Đang giữ chỗ: <strong>{active}</strong>
            {event.capacity ? ` / ${event.capacity}` : " (không giới hạn)"}
          </Typography.Text>
          <Typography.Text>
            Đã check-in: <strong>{checkedIn}</strong>
          </Typography.Text>
          {/* Mở tab mới để chiếu lên màn hình lớn, tab quản lý vẫn giữ để check-in tay */}
          {ongoing && (
            <Link
              to={`/app/events/${event.id}/check-in-screen`}
              target="_blank"
            >
              <Button type="primary" icon={<QrcodeOutlined />}>
                Mở màn hình QR check-in
              </Button>
            </Link>
          )}
        </Flex>
        <Input.Search
          allowClear
          placeholder="Tìm theo tên, email, MSSV"
          onChange={(e) => setKeyword(e.target.value)}
          style={{ maxWidth: 300 }}
        />
      </Flex>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={shown}
        loading={registrations.isPending}
        pagination={{ pageSize: 50, hideOnSinglePage: true }}
        scroll={{ x: true }}
        locale={{ emptyText: "Chưa có ai đăng ký" }}
      />
    </Flex>
  );
}
