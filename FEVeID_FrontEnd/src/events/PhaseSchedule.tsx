import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import {
  Alert,
  Button,
  DatePicker,
  Flex,
  Form,
  Input,
  Popconfirm,
  Select,
  Table,
  Tag,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useState } from "react";
import type { EventResponse, PhaseKind, PhaseResponse } from "../api/types";
import { formatDateTime, isFuture, toPickerValue } from "../lib/time";
import { replacePhases, startPhaseNow } from "./eventApi";
import { isLocked, PHASE_KIND_LABELS } from "./eventLabels";
import { useEventMutation } from "./eventQueries";

type PhaseRow = { name: string; startsAt?: Dayjs; kind: PhaseKind };
type ScheduleValues = { phases: PhaseRow[] };

// Sự kiện chưa có lịch → gợi ý sẵn 3 dòng tối thiểu, chỉ cần điền giờ
const DEFAULT_ROWS: PhaseRow[] = [
  { name: "Sắp diễn ra", kind: "UPCOMING" },
  { name: "Đang diễn ra", kind: "ONGOING" },
  { name: "Đã kết thúc", kind: "COMPLETED" },
];

const KIND_OPTIONS = (Object.keys(PHASE_KIND_LABELS) as PhaseKind[]).map(
  (kind) => ({
    value: kind,
    label: PHASE_KIND_LABELS[kind],
  }),
);

// Giai đoạn đang chạy = giai đoạn cuối cùng đã tới giờ (giống EventStatusCalculator bên backend)
function currentPhaseId(event: EventResponse): number | undefined {
  if (event.publication !== "PUBLISHED") return undefined;
  const now = dayjs();
  return event.phases
    .filter((phase) => !dayjs(phase.startsAt).isAfter(now))
    .at(-1)?.id;
}

export function PhaseSchedule({ event }: { event: EventResponse }) {
  const [editing, setEditing] = useState(false);
  const locked = isLocked(event);

  const save = useEventMutation(
    (rows: PhaseRow[]) =>
      replacePhases(
        event.id,
        rows.map((row) => ({
          name: row.name,
          startsAt: row.startsAt!.toISOString(),
          kind: row.kind,
        })),
      ),
    "Đã lưu lịch giai đoạn",
  );
  const startNow = useEventMutation(
    (phaseId: number) => startPhaseNow(event.id, phaseId),
    "Đã chuyển giai đoạn",
  );

  const activeId = currentPhaseId(event);
  const columns: TableColumnsType<PhaseResponse> = [
    {
      title: "Giai đoạn",
      dataIndex: "name",
      render: (name: string, phase) => (
        <>
          {name} {phase.id === activeId && <Tag color="green">Đang chạy</Tag>}
        </>
      ),
    },
    {
      title: "Loại",
      dataIndex: "kind",
      render: (kind: PhaseKind) => PHASE_KIND_LABELS[kind],
    },
    {
      title: "Bắt đầu (giờ VN)",
      dataIndex: "startsAt",
      render: (iso: string) => formatDateTime(iso),
    },
    {
      title: "",
      key: "action",
      // Chỉ sự kiện đã công bố + giai đoạn chưa tới giờ (giống điều kiện trong startPhaseNow bên backend)
      render: (_, phase) =>
        event.publication === "PUBLISHED" &&
        !locked &&
        isFuture(phase.startsAt) && (
          <Popconfirm
            title={`Bắt đầu "${phase.name}" ngay bây giờ?`}
            description="Giờ bắt đầu của giai đoạn này sẽ đổi thành bây giờ"
            cancelText="Không"
            onConfirm={() => startNow.mutate(phase.id)}
          >
            <Button
              size="small"
              loading={startNow.isPending && startNow.variables === phase.id}
            >
              Bắt đầu ngay
            </Button>
          </Popconfirm>
        ),
    },
  ];

  if (!editing) {
    return (
      <Flex vertical gap={16}>
        {event.phases.length === 0 ? (
          <Alert
            type="info"
            title="Chưa có lịch giai đoạn. Cần lập lịch trước khi công bố."
          />
        ) : (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={event.phases}
            pagination={false}
            scroll={{ x: 600 }}
          />
        )}
        <div>
          <Button
            icon={<EditOutlined />}
            disabled={locked}
            onClick={() => setEditing(true)}
          >
            {event.phases.length === 0 ? "Lập lịch" : "Sửa lịch"}
          </Button>
        </div>
      </Flex>
    );
  }

  const initialRows: PhaseRow[] =
    event.phases.length === 0
      ? DEFAULT_ROWS
      : event.phases.map((phase) => ({
          name: phase.name,
          startsAt: toPickerValue(phase.startsAt),
          kind: phase.kind,
        }));

  return (
    <Form<ScheduleValues>
      layout="vertical"
      initialValues={{ phases: initialRows }}
      onFinish={({ phases }) =>
        // Backend bắt giờ tăng dần → tự xếp theo giờ trước khi gửi, người dùng thêm dòng ở đâu cũng được
        save.mutate(
          [...phases].sort(
            (a, b) => a.startsAt!.valueOf() - b.startsAt!.valueOf(),
          ),
          {
            onSuccess: () => setEditing(false),
          },
        )
      }
    >
      <Typography.Paragraph type="secondary">
        Mỗi giai đoạn chạy từ giờ bắt đầu của nó tới giờ bắt đầu của giai đoạn
        sau. Giai đoạn đầu tiên phải là loại "Sắp diễn ra"; phải có ít nhất một
        giai đoạn "Đang diễn ra" và một "Kết thúc"; loại không được đi lùi.
      </Typography.Paragraph>

      {/* Form.List = form có số dòng thay đổi được (giống List<PhaseRequest> bên Java).
          fields: các dòng hiện có; add/remove: thêm/xóa dòng. Mỗi ô có name={[field.name, "tên field"]}
          → giá trị nằm ở phases[0].name, phases[0].startsAt... */}
      <Form.List name="phases">
        {(fields, { add, remove }) => (
          <Flex vertical gap={8}>
            {fields.map((field) => (
              <Flex key={field.key} gap={8} wrap align="flex-start">
                <Form.Item
                  name={[field.name, "name"]}
                  rules={[
                    { required: true, whitespace: true, message: "Nhập tên" },
                    { max: 100, message: "Tối đa 100 ký tự" },
                  ]}
                  style={{ marginBottom: 0, flex: "1 1 200px" }}
                >
                  <Input placeholder="Tên giai đoạn, vd Check-in" />
                </Form.Item>
                <Form.Item
                  name={[field.name, "startsAt"]}
                  rules={[{ required: true, message: "Chọn giờ" }]}
                  style={{ marginBottom: 0 }}
                >
                  <DatePicker
                    showTime={{ format: "HH:mm" }}
                    format="DD/MM/YYYY HH:mm"
                    placeholder="Giờ bắt đầu"
                  />
                </Form.Item>
                <Form.Item
                  name={[field.name, "kind"]}
                  style={{ marginBottom: 0, width: 150 }}
                >
                  <Select options={KIND_OPTIONS} />
                </Form.Item>
                <Button
                  icon={<DeleteOutlined />}
                  aria-label="Xóa giai đoạn"
                  disabled={fields.length <= 1}
                  onClick={() => remove(field.name)}
                />
              </Flex>
            ))}
            <div>
              <Button
                type="dashed"
                icon={<PlusOutlined />}
                disabled={fields.length >= 20} // PhaseScheduleRequest: @Size(max = 20)
                onClick={() => add({ name: "", kind: "ONGOING" })}
              >
                Thêm giai đoạn
              </Button>
            </div>
          </Flex>
        )}
      </Form.List>

      <Flex gap={8} style={{ marginTop: 16 }}>
        <Button type="primary" htmlType="submit" loading={save.isPending}>
          Lưu lịch
        </Button>
        <Button onClick={() => setEditing(false)}>Hủy sửa</Button>
      </Flex>
    </Form>
  );
}
