import { EnvironmentOutlined, ClockCircleOutlined } from "@ant-design/icons";
import {
  Alert,
  Card,
  Col,
  Empty,
  Flex,
  Input,
  Row,
  Segmented,
  Spin,
  Tag,
  Typography,
} from "antd";
import { Link, useSearchParams } from "react-router";
import { errorMessage } from "../../api/client";
import type { EventStatus, PublicEventResponse } from "../../api/types";
import { bannerThumb, STATUS_TAGS } from "../../events/eventLabels";
import { formatDateTime } from "../../lib/time";
import { usePublicEvents } from "../../registrations/registrationQueries";

type StatusFilter =
  "ALL" | Extract<EventStatus, "UPCOMING" | "ONGOING" | "COMPLETED">;

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "UPCOMING", label: STATUS_TAGS.UPCOMING.label },
  { value: "ONGOING", label: STATUS_TAGS.ONGOING.label },
  { value: "COMPLETED", label: STATUS_TAGS.COMPLETED.label },
];

// /events?status=UPCOMING&q=nhạc
// useSearchParams ≈ @RequestParam: bộ lọc nằm trên URL nên gửi link cho người khác vẫn giữ nguyên bộ lọc
export function PublicEventListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const status = (searchParams.get("status") ?? "ALL") as StatusFilter;
  const q = searchParams.get("q") ?? "";

  const events = usePublicEvents({
    status: status === "ALL" ? undefined : status,
    q,
  });

  // Đổi một tham số, giữ tham số còn lại; giá trị rỗng thì bỏ khỏi URL cho gọn
  function updateParam(name: "status" | "q", value: string) {
    const next = new URLSearchParams(searchParams);
    if (value && value !== "ALL") next.set(name, value);
    else next.delete(name);
    setSearchParams(next, { replace: true });
  }

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={2} style={{ margin: 0 }}>
        Sự kiện
      </Typography.Title>

      <Flex gap={12} wrap>
        <Segmented
          options={STATUS_OPTIONS}
          value={status}
          onChange={(value) => updateParam("status", value)}
        />
        {/* key={q}: URL đổi (vd bấm Back) → ô tìm kiếm hiện đúng từ khóa trên URL */}
        <Input.Search
          key={q}
          defaultValue={q}
          placeholder="Tìm theo tên sự kiện"
          allowClear
          onSearch={(value) => updateParam("q", value.trim())}
          style={{ maxWidth: 320 }}
        />
      </Flex>

      {events.isPending ? (
        <Spin size="large" style={{ display: "block", margin: "64px auto" }} />
      ) : events.isError ? (
        <Alert type="error" title={errorMessage(events.error)} />
      ) : events.data.length === 0 ? (
        <Empty description="Chưa có sự kiện nào phù hợp" />
      ) : (
        // Row/Col: lưới 24 cột. Điện thoại 1 thẻ/hàng, máy tính bảng 2, máy tính 3
        <Row gutter={[16, 16]}>
          {events.data.map((event) => (
            <Col key={event.id} xs={24} sm={12} lg={8}>
              <EventCard event={event} />
            </Col>
          ))}
        </Row>
      )}
    </Flex>
  );
}

function EventCard({ event }: { event: PublicEventResponse }) {
  const status = STATUS_TAGS[event.status];
  return (
    <Link to={`/events/${event.id}`}>
      <Card
        hoverable
        cover={
          event.bannerUrl ? (
            <img
              src={bannerThumb(event.bannerUrl, 600)}
              alt={event.name}
              style={{ aspectRatio: "16 / 9", objectFit: "cover" }}
            />
          ) : (
            <div style={{ aspectRatio: "16 / 9", background: "#f3e8ff" }} />
          )
        }
      >
        <Flex vertical gap={8}>
          <Flex gap={4} wrap>
            <Tag color={status.color}>{status.label}</Tag>
            {event.registrationOpen && <Tag color="green">Đang mở đăng ký</Tag>}
            {event.visibility === "INTERNAL" && <Tag>Chỉ thành viên</Tag>}
          </Flex>
          <Typography.Title
            level={5}
            style={{ margin: 0 }}
            ellipsis={{ rows: 2 }}
          >
            {event.name}
          </Typography.Title>
          <Typography.Text type="secondary">
            <ClockCircleOutlined /> {formatDateTime(event.startsAt)}
          </Typography.Text>
          {event.location && (
            <Typography.Text type="secondary" ellipsis>
              <EnvironmentOutlined /> {event.location}
            </Typography.Text>
          )}
        </Flex>
      </Card>
    </Link>
  );
}
