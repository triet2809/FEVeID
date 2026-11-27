import { DeleteOutlined } from "@ant-design/icons";
import { Alert, Button, Flex, Select, Spin, Table, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useState } from "react";
import { errorMessage } from "../api/client";
import type {
  EventResponse,
  EventStaffResponse,
  StaffRole,
} from "../api/types";
import { STAFF_ROLE_LABELS } from "./eventLabels";
import {
  useEventStaff,
  useMembers,
  useReplaceEventStaff,
} from "./eventQueries";

const ROLE_OPTIONS = (["ORGANIZER", "SUPPORTER"] as const).map((role) => ({
  value: role,
  label: STAFF_ROLE_LABELS[role].label,
}));

// Tab "Team" (requirements 5.2): phân công thành viên từ nhiều ban vào team tổ chức/hỗ trợ.
// Lúc chốt sự kiện, team này được tick sẵn (+4 tổ chức, +2 hỗ trợ)
export function StaffSection({ event }: { event: EventResponse }) {
  const staff = useEventStaff(event.id);

  if (staff.isPending) {
    return <Spin style={{ display: "block", margin: "32px auto" }} />;
  }
  if (staff.isError) {
    return <Alert type="error" title={errorMessage(staff.error)} />;
  }
  // key: lưu xong backend trả danh sách mới → key đổi → StaffEditor tạo lại với dữ liệu mới
  // (giống new một object mới thay vì sửa từng field của object cũ)
  return (
    <StaffEditor
      key={staff.data.map((s) => `${s.memberId}:${s.role}`).join(",")}
      event={event}
      initial={staff.data}
    />
  );
}

function StaffEditor({
  event,
  initial,
}: {
  event: EventResponse;
  initial: EventStaffResponse[];
}) {
  const members = useMembers();
  const save = useReplaceEventStaff(event.id);
  // Bản nháp trên trình duyệt: thêm/bỏ/đổi vai trò thoải mái, bấm "Lưu team" mới gửi PUT cả danh sách
  const [team, setTeam] = useState<EventStaffResponse[]>(initial);
  const [pickedMember, setPickedMember] = useState<number>();
  const [pickedRole, setPickedRole] = useState<StaffRole>("SUPPORTER");

  // Khớp luật backend (EventStaffServiceImpl): hủy hoặc đã chốt thì không sửa team được
  const readOnly = event.publication === "CANCELLED" || !!event.finalizedAt;
  const changed =
    JSON.stringify(team.map((s) => [s.memberId, s.role])) !==
    JSON.stringify(initial.map((s) => [s.memberId, s.role]));

  const inTeam = new Set(team.map((s) => s.memberId));
  // Người đã bị loại thì backend từ chối → không cho chọn luôn
  const memberOptions = (members.data ?? [])
    .filter((m) => !inTeam.has(m.id) && m.memberStatus !== "DISQUALIFIED")
    .map((m) => ({ value: m.id, label: `${m.fullName} · ${m.email}` }));

  function addMember() {
    const member = members.data?.find((m) => m.id === pickedMember);
    if (!member) return;
    setTeam([
      ...team,
      {
        memberId: member.id,
        fullName: member.fullName,
        studentCode: member.studentCode,
        role: pickedRole,
      },
    ]);
    setPickedMember(undefined);
  }

  function changeRole(memberId: number, role: StaffRole) {
    setTeam(team.map((s) => (s.memberId === memberId ? { ...s, role } : s)));
  }

  const columns: TableColumnsType<EventStaffResponse> = [
    { title: "Họ tên", dataIndex: "fullName" },
    {
      title: "MSSV",
      dataIndex: "studentCode",
      render: (v?: string) => v ?? "—",
    },
    {
      title: "Vai trò",
      dataIndex: "role",
      render: (role: StaffRole, row) => (
        <Select
          value={role}
          options={ROLE_OPTIONS}
          disabled={readOnly}
          style={{ width: 120 }}
          onChange={(value: StaffRole) => changeRole(row.memberId, value)}
        />
      ),
    },
    {
      title: "",
      key: "remove",
      width: 56,
      render: (_, row) =>
        !readOnly && (
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            aria-label={`Gỡ ${row.fullName}`}
            onClick={() =>
              setTeam(team.filter((s) => s.memberId !== row.memberId))
            }
          />
        ),
    },
  ];

  return (
    <Flex vertical gap={12}>
      {readOnly && (
        <Alert
          type="info"
          title={
            event.finalizedAt
              ? "Sự kiện đã chốt, team được giữ nguyên như lúc chốt."
              : "Sự kiện đã hủy nên không sửa team được."
          }
        />
      )}

      {!readOnly && (
        <Flex gap={8} wrap align="center">
          <Select
            showSearch={{ optionFilterProp: "label" }} // gõ tên hoặc email để lọc
            allowClear
            placeholder="Thêm thành viên (gõ tên hoặc email)"
            style={{ minWidth: 320 }}
            loading={members.isPending}
            value={pickedMember}
            options={memberOptions}
            onChange={(id?: number) => setPickedMember(id)}
          />
          <Select
            value={pickedRole}
            options={ROLE_OPTIONS}
            style={{ width: 120 }}
            onChange={(role: StaffRole) => setPickedRole(role)}
          />
          <Button onClick={addMember} disabled={pickedMember === undefined}>
            Thêm
          </Button>
        </Flex>
      )}

      <Table
        rowKey="memberId"
        columns={columns}
        dataSource={team}
        pagination={false}
        scroll={{ x: true }}
        locale={{ emptyText: "Chưa phân công ai" }}
      />

      {!readOnly && (
        <Flex gap={12} align="center">
          <Button
            type="primary"
            disabled={!changed}
            loading={save.isPending}
            onClick={() =>
              save.mutate(
                team.map(({ memberId, role }) => ({ memberId, role })),
              )
            }
          >
            Lưu team
          </Button>
          {changed && (
            <Typography.Text type="warning">
              Có thay đổi chưa lưu
            </Typography.Text>
          )}
        </Flex>
      )}
    </Flex>
  );
}
