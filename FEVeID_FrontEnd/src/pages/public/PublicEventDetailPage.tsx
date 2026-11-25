import { ArrowLeftOutlined } from "@ant-design/icons";
import {
  Card,
  Col,
  Descriptions,
  Flex,
  Result,
  Row,
  Spin,
  Tag,
  Timeline,
  Typography,
} from "antd";
import { Link, useParams } from "react-router";
import { errorMessage, HttpError } from "../../api/client";
import type { PublicEventResponse } from "../../api/types";
import {
  bannerThumb,
  phaseNameToShow,
  STATUS_TAGS,
} from "../../events/eventLabels";
import { formatDateTime } from "../../lib/time";
import { RegistrationPanel } from "../../registrations/RegistrationPanel";
import { usePublicEvent } from "../../registrations/registrationQueries";

// /events/:id: ai cũng mở được. Khách mở sự kiện nội bộ bị ẩn → backend trả 404 như không tồn tại
export function PublicEventDetailPage() {
  const id = Number(useParams().id);
  const event = usePublicEvent(id);

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
        extra={<Link to="/events">Xem các sự kiện khác</Link>}
      />
    );
  }
  return <PublicEventDetail event={event.data} />;
}

function PublicEventDetail({ event }: { event: PublicEventResponse }) {
  const status = STATUS_TAGS[event.status];

  return (
    <Flex vertical gap={16}>
      <div>
        <Link to="/events">
          <ArrowLeftOutlined /> Tất cả sự kiện
        </Link>
      </div>

      {event.bannerUrl && (
        <img
          src={bannerThumb(event.bannerUrl, 1200)}
          alt={event.name}
          style={{
            width: "100%",
            maxHeight: 420,
            objectFit: "cover",
            borderRadius: 8,
          }}
        />
      )}

      <Flex vertical gap={8}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          {event.name}
        </Typography.Title>
        <Flex gap={8} wrap align="center">
          <Tag color={status.color}>{status.label}</Tag>
          {phaseNameToShow(event) && (
            <Typography.Text>{phaseNameToShow(event)}</Typography.Text>
          )}
          {event.visibility === "INTERNAL" && (
            <Tag>Chỉ dành cho thành viên CLB</Tag>
          )}
        </Flex>
      </Flex>

      {/* Máy tính: thông tin bên trái (16/24), ô đăng ký bên phải (8/24). Điện thoại: xếp chồng, ô đăng ký lên trên */}
      <Row gutter={[24, 24]}>
        <Col xs={{ span: 24, order: 2 }} lg={{ span: 16, order: 1 }}>
          <Flex vertical gap={16}>
            <Descriptions
              column={1}
              bordered
              size="small"
              items={[
                {
                  key: "time",
                  label: "Thời gian",
                  children: `${formatDateTime(event.startsAt)} → ${formatDateTime(event.endsAt)}`,
                },
                {
                  key: "location",
                  label: "Địa điểm",
                  children: event.location ?? "—",
                },
                {
                  key: "registration",
                  label: "Đăng ký",
                  children: `${formatDateTime(event.registrationOpensAt)} → ${formatDateTime(event.registrationClosesAt)}`,
                },
                {
                  key: "capacity",
                  label: "Số chỗ",
                  children: event.capacity ?? "Không giới hạn",
                },
              ]}
            />

            {event.description && (
              // pre-wrap: giữ nguyên xuống dòng HR/BCN đã gõ trong ô mô tả
              <Typography.Paragraph style={{ whiteSpace: "pre-wrap" }}>
                {event.description}
              </Typography.Paragraph>
            )}

            <Card title="Lịch trình" size="small">
              <Timeline
                items={event.phases.map((phase) => ({
                  // Tô xanh giai đoạn đang diễn ra
                  color:
                    phase.name === event.currentPhaseName ? "blue" : "gray",
                  content: (
                    <>
                      <Typography.Text strong>{phase.name}</Typography.Text>
                      <br />
                      <Typography.Text type="secondary">
                        {formatDateTime(phase.startsAt)}
                      </Typography.Text>
                    </>
                  ),
                }))}
              />
            </Card>
          </Flex>
        </Col>

        <Col xs={{ span: 24, order: 1 }} lg={{ span: 8, order: 2 }}>
          <Card title="Đăng ký tham gia">
            <RegistrationPanel event={event} />
          </Card>
        </Col>
      </Row>
    </Flex>
  );
}
