import {
  Alert,
  App,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Flex,
  Spin,
  Statistic,
  Typography,
} from "antd";
import { useState } from "react";
import { errorMessage } from "../api/client";
import type {
  EventResponse,
  FinalizationPerson,
  FinalizationResponse,
} from "../api/types";
import { formatDateTime } from "../lib/time";
import { useFinalization, useFinalizeEvent } from "./eventQueries";

function personLabel(p: FinalizationPerson): string {
  return p.studentCode ? `${p.fullName} (${p.studentCode})` : p.fullName;
}

// Tab "Chốt sự kiện" (requirements 11.1). Luồng:
//   GET /finalization → xem trước (team tick sẵn, người đã check-in, điểm dự kiến)
//   HR bỏ/giữ tick → bấm "Chốt" → hỏi lại → POST /finalization → backend cộng điểm + NO_SHOW + khóa sự kiện
//   Đã chốt → GET trả kết quả đọc từ sổ cái (chỉ xem)
export function FinalizationSection({ event }: { event: EventResponse }) {
  const finalization = useFinalization(event.id);

  if (finalization.isPending) {
    return <Spin style={{ display: "block", margin: "32px auto" }} />;
  }
  if (finalization.isError) {
    return <Alert type="error" title={errorMessage(finalization.error)} />;
  }
  const data = finalization.data;
  if (data.finalized) {
    return <FinalizedResult data={data} />;
  }
  // key: team hoặc check-in đổi (lưu team, check-in bù) → tick lại từ đầu theo dữ liệu mới
  return (
    <FinalizeForm
      key={[...data.organizers, ...data.supporters, ...data.attendees]
        .map((p) => p.memberId)
        .join(",")}
      event={event}
      data={data}
    />
  );
}

function FinalizeForm({
  event,
  data,
}: {
  event: EventResponse;
  data: FinalizationResponse;
}) {
  const { modal } = App.useApp();
  const finalize = useFinalizeEvent(event.id);
  const [organizerIds, setOrganizerIds] = useState<number[]>(
    data.organizers.map((p) => p.memberId),
  );
  const [supporterIds, setSupporterIds] = useState<number[]>(
    data.supporters.map((p) => p.memberId),
  );

  // Điểm dự kiến theo lựa chọn hiện tại (backend vẫn tự tính lại khi chốt)
  const total =
    organizerIds.length * data.organizePoints +
    supporterIds.length * data.supportPoints +
    data.attendees.length * data.attendPoints;

  function confirmFinalize() {
    modal.confirm({
      title: "Chốt sự kiện?",
      content: (
        <Flex vertical gap={4}>
          <span>
            Cộng <strong>{total}</strong> điểm vào kỳ{" "}
            <strong>{data.termName}</strong> cho{" "}
            {organizerIds.length + supporterIds.length + data.attendees.length}{" "}
            thành viên.
          </span>
          <span>
            {data.noShowCount} đăng ký không đến sẽ chuyển sang "Không đến".
          </span>
          <Typography.Text type="danger">
            Chốt xong không hoàn tác được: không sửa team, không check-in thêm.
          </Typography.Text>
        </Flex>
      ),
      okText: "Chốt sự kiện",
      cancelText: "Không",
      // Trả về Promise: nút OK hiện loading tới khi backend trả lời, lỗi thì hộp thoại vẫn mở
      onOk: () => finalize.mutateAsync({ organizerIds, supporterIds }),
    });
  }

  return (
    <Flex vertical gap={16}>
      {!data.finalizable && (
        <Alert
          type="info"
          title={data.blockedReason}
          description="Dưới đây là bản xem trước theo team và check-in hiện tại."
        />
      )}

      <Descriptions size="small" column={{ xs: 1, md: 3 }} bordered>
        <Descriptions.Item label="Kỳ nhận điểm">
          {data.termName ?? "—"}
        </Descriptions.Item>
        <Descriptions.Item label="Khách đã check-in (không có điểm)">
          {data.guestAttendeeCount}
        </Descriptions.Item>
        <Descriptions.Item label="Sẽ chuyển sang Không đến">
          {data.noShowCount}
        </Descriptions.Item>
      </Descriptions>

      <Flex gap={16} wrap>
        <PeopleCard
          title={`Tổ chức (+${data.organizePoints})`}
          people={data.organizers}
          checked={organizerIds}
          onChange={setOrganizerIds}
          disabled={!data.finalizable}
          empty="Team chưa có người tổ chức"
        />
        <PeopleCard
          title={`Hỗ trợ (+${data.supportPoints})`}
          people={data.supporters}
          checked={supporterIds}
          onChange={setSupporterIds}
          disabled={!data.finalizable}
          empty="Team chưa có người hỗ trợ"
        />
        <PeopleCard
          title={`Tham gia (+${data.attendPoints}, từ check-in)`}
          people={data.attendees}
          empty="Chưa có thành viên nào check-in"
        />
      </Flex>

      <Typography.Text type="secondary">
        Muốn thêm người vào team thì sửa ở tab "Team". Người trong team bị bỏ
        tick mà có check-in vẫn nhận điểm tham gia. Ai có đến nhưng quên quét mã
        thì check-in bù ở tab "Đăng ký & check-in".
      </Typography.Text>

      <Flex gap={16} align="center" wrap>
        <Statistic title="Tổng điểm dự kiến" value={total} />
        <Button
          type="primary"
          size="large"
          disabled={!data.finalizable}
          loading={finalize.isPending}
          onClick={confirmFinalize}
        >
          Chốt sự kiện
        </Button>
      </Flex>
    </Flex>
  );
}

// Một cột người: có checked/onChange thì là danh sách tick (team), không có thì chỉ xem (người tham gia)
function PeopleCard({
  title,
  people,
  checked,
  onChange,
  disabled,
  empty,
}: {
  title: string;
  people: FinalizationPerson[];
  checked?: number[];
  onChange?: (ids: number[]) => void;
  disabled?: boolean;
  empty: string;
}) {
  return (
    <Card
      size="small"
      title={`${title} · ${people.length}`}
      style={{ flex: "1 1 260px" }}
    >
      {people.length === 0 ? (
        <Typography.Text type="secondary">{empty}</Typography.Text>
      ) : onChange ? (
        <Checkbox.Group
          value={checked}
          disabled={disabled}
          onChange={(ids) => onChange(ids as number[])}
          options={people.map((p) => ({
            value: p.memberId,
            label: personLabel(p),
          }))}
          style={{ display: "flex", flexDirection: "column", gap: 4 }}
        />
      ) : (
        // Danh sách chỉ xem: không dùng List của antd (đã lỗi thời từ 6.6)
        <Flex vertical gap={4}>
          {people.map((p) => (
            <span key={p.memberId}>{personLabel(p)}</span>
          ))}
        </Flex>
      )}
    </Card>
  );
}

// Đã chốt: chỉ xem, số liệu đọc từ sổ cái điểm
function FinalizedResult({ data }: { data: FinalizationResponse }) {
  return (
    <Flex vertical gap={16}>
      <Alert
        type="success"
        title={`Đã chốt lúc ${formatDateTime(data.finalizedAt)} bởi ${data.finalizedByName}`}
        description={`Đã phát ${data.totalPoints} điểm vào kỳ ${data.termName}.`}
      />
      <Descriptions size="small" column={{ xs: 1, md: 2 }} bordered>
        <Descriptions.Item label="Khách đã check-in (không có điểm)">
          {data.guestAttendeeCount}
        </Descriptions.Item>
        <Descriptions.Item label="Không đến">
          {data.noShowCount}
        </Descriptions.Item>
      </Descriptions>
      <Flex gap={16} wrap>
        <PeopleCard title="Tổ chức" people={data.organizers} empty="Không có" />
        <PeopleCard title="Hỗ trợ" people={data.supporters} empty="Không có" />
        <PeopleCard title="Tham gia" people={data.attendees} empty="Không có" />
      </Flex>
    </Flex>
  );
}
