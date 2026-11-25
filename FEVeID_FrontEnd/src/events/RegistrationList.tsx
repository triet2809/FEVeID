import { Alert, Flex, Input, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useState } from "react";
import { errorMessage } from "../api/client";
import type { EventRegistrationResponse, EventResponse } from "../api/types";
import { formatDateTime } from "../lib/time";
import {
  isActiveRegistration,
  REGISTRATION_STATUS_TAGS,
} from "../registrations/registrationLabels";
import { useEventRegistrations } from "../registrations/registrationQueries";

const columns: TableColumnsType<EventRegistrationResponse> = [
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

// Tab "Đăng ký" trong trang quản lý sự kiện (HR/BCN)
export function RegistrationList({ event }: { event: EventResponse }) {
  const registrations = useEventRegistrations(event.id);
  const [keyword, setKeyword] = useState("");

  if (registrations.isError) {
    return <Alert type="error" title={errorMessage(registrations.error)} />;
  }

  const rows = registrations.data ?? [];
  const active = rows.filter((r) => isActiveRegistration(r.status)).length;
  const shown = keyword ? rows.filter((r) => matches(r, keyword)) : rows;

  return (
    <Flex vertical gap={12}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Typography.Text>
          Đang giữ chỗ: <strong>{active}</strong>
          {event.capacity ? ` / ${event.capacity}` : " (không giới hạn)"}
        </Typography.Text>
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
