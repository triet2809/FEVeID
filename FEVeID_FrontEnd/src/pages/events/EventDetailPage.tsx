import { ArrowLeftOutlined } from "@ant-design/icons";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  App,
  Button,
  Card,
  Flex,
  Popconfirm,
  Result,
  Spin,
  Tabs,
  Tag,
  Typography,
} from "antd";
import { Link, useNavigate, useParams } from "react-router";
import { errorMessage, HttpError } from "../../api/client";
import type { EventRequest, EventResponse } from "../../api/types";
import { BannerSection } from "../../events/BannerSection";
import {
  cancelEvent,
  deleteEvent,
  publishEvent,
  updateEvent,
} from "../../events/eventApi";
import { EventInfoForm } from "../../events/EventInfoForm";
import {
  isLocked,
  phaseNameToShow,
  STATUS_TAGS,
  VISIBILITY_LABELS,
} from "../../events/eventLabels";
import {
  EVENTS_KEY,
  eventKey,
  useEvent,
  useEventMutation,
} from "../../events/eventQueries";
import { PhaseSchedule } from "../../events/PhaseSchedule";
import { formatDateTime } from "../../lib/time";

// /app/events/:id  (useParams ≈ @PathVariable, nhưng luôn là chuỗi → tự đổi sang số)
export function EventDetailPage() {
  const id = Number(useParams().id);
  const event = useEvent(id);

  if (event.isPending) {
    return (
      <Spin size="large" style={{ display: "block", margin: "64px auto" }} />
    );
  }
  if (event.isError) {
    const notFound =
      event.error instanceof HttpError && event.error.status === 404;
    return (
      <Result
        status={notFound ? "404" : "error"}
        title={notFound ? "Không tìm thấy sự kiện" : errorMessage(event.error)}
        extra={<Link to="/app/events">Về danh sách sự kiện</Link>}
      />
    );
  }
  // Tách component con: tới đây chắc chắn đã có data → bên dưới khỏi kiểm tra undefined
  return <EventDetail event={event.data} />;
}

function EventDetail({ event }: { event: EventResponse }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const locked = isLocked(event);

  const update = useEventMutation(
    (request: EventRequest) => updateEvent(event.id, request),
    "Đã lưu thông tin",
  );
  const publish = useEventMutation(
    () => publishEvent(event.id),
    "Đã công bố sự kiện",
  );
  const cancel = useEventMutation(
    () => cancelEvent(event.id),
    "Đã hủy sự kiện",
  );
  // Xóa không trả về EventResponse nên không dùng useEventMutation: bỏ khỏi cache rồi về danh sách
  const remove = useMutation({
    mutationFn: () => deleteEvent(event.id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: eventKey(event.id) });
      queryClient.invalidateQueries({ queryKey: EVENTS_KEY, exact: true });
      message.success("Đã xóa bản nháp");
      navigate("/app/events");
    },
    onError: (err) => message.error(errorMessage(err)),
  });

  const status = STATUS_TAGS[event.status];

  return (
    <Flex vertical gap={16}>
      <div>
        <Link to="/app/events">
          <ArrowLeftOutlined /> Danh sách sự kiện
        </Link>
      </div>

      <Flex justify="space-between" align="flex-start" wrap gap={12}>
        <Flex vertical gap={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {event.name}
          </Typography.Title>
          <Flex gap={8} wrap align="center">
            <Tag color={status.color}>{status.label}</Tag>
            {phaseNameToShow(event) && (
              <Typography.Text>{phaseNameToShow(event)}</Typography.Text>
            )}
            <Typography.Text type="secondary">
              {VISIBILITY_LABELS[event.visibility]} ·{" "}
              {formatDateTime(event.startsAt)} → {formatDateTime(event.endsAt)}
            </Typography.Text>
          </Flex>
        </Flex>

        {/* Nút nào hiện tùy trạng thái, khớp với luật bên EventServiceImpl */}
        <Flex gap={8} wrap>
          {event.publication === "DRAFT" && (
            <Popconfirm
              title="Công bố sự kiện?"
              description="Sau khi công bố, sự kiện chạy theo lịch giai đoạn và không xóa được nữa (chỉ hủy được)"
              cancelText="Không" // mặc định là "Hủy", dễ nhầm với nút "Hủy sự kiện" ngay bên cạnh
              onConfirm={() => publish.mutate(undefined)}
            >
              <Button type="primary" loading={publish.isPending}>
                Công bố
              </Button>
            </Popconfirm>
          )}
          {!locked && (
            <Popconfirm
              title="Hủy sự kiện?"
              description="Hủy là vĩnh viễn, không mở lại được"
              okText="Hủy sự kiện"
              okButtonProps={{ danger: true }}
              cancelText="Không"
              onConfirm={() => cancel.mutate(undefined)}
            >
              <Button danger loading={cancel.isPending}>
                Hủy sự kiện
              </Button>
            </Popconfirm>
          )}
          {event.publication === "DRAFT" && (
            <Popconfirm
              title="Xóa bản nháp?"
              description="Xóa luôn lịch giai đoạn và ảnh banner"
              okText="Xóa"
              okButtonProps={{ danger: true }}
              cancelText="Không"
              onConfirm={() => remove.mutate()}
            >
              <Button danger type="text" loading={remove.isPending}>
                Xóa
              </Button>
            </Popconfirm>
          )}
        </Flex>
      </Flex>

      {locked && (
        <Alert
          type="warning"
          title="Sự kiện đã kết thúc hoặc đã hủy nên không chỉnh sửa được nữa."
        />
      )}

      <Card>
        {/* Tabs: mỗi tab gọi một API riêng (PUT /events/{id}, PUT /phases, PUT /banner) như bên backend */}
        <Tabs
          items={[
            {
              key: "info",
              label: "Thông tin",
              children: (
                <EventInfoForm
                  event={event}
                  submitText="Lưu thông tin"
                  submitting={update.isPending}
                  disabled={locked}
                  onSubmit={(request) => update.mutate(request)}
                />
              ),
            },
            {
              key: "phases",
              label: "Lịch giai đoạn",
              children: <PhaseSchedule event={event} />,
            },
            {
              key: "banner",
              label: "Ảnh banner",
              children: <BannerSection event={event} />,
            },
          ]}
        />
      </Card>
    </Flex>
  );
}
