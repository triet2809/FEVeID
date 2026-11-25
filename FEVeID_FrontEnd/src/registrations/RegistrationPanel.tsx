import { useMutation } from "@tanstack/react-query";
import {
  Alert,
  App,
  Button,
  Flex,
  Form,
  Input,
  Popconfirm,
  Result,
  Spin,
  Tag,
  Typography,
} from "antd";
import { Link, useLocation } from "react-router";
import { errorMessage } from "../api/client";
import type {
  GuestRegistrationRequest,
  PublicEventResponse,
} from "../api/types";
import { useMe } from "../auth/useMe";
import { formatDateTime, isFuture } from "../lib/time";
import { registerAsGuest, registerAsMember } from "./registrationApi";
import {
  isActiveRegistration,
  REGISTRATION_STATUS_TAGS,
} from "./registrationLabels";
import {
  useCancelRegistration,
  useMyRegistrations,
  useRegistrationMutation,
} from "./registrationQueries";

// Ô "Đăng ký tham gia" bên phải trang chi tiết. Ai đang xem quyết định hiện gì:
//   member (đã đăng nhập)      → nút Đăng ký / đã đăng ký + nút Hủy
//   khách + sự kiện PUBLIC     → form nhập họ tên, email, SĐT, MSSV
//   khách + sự kiện INTERNAL   → mời đăng nhập
// Backend vẫn kiểm tra lại mọi luật (giờ đăng ký, trùng, hết chỗ); ở đây chỉ để hiện cho đúng
export function RegistrationPanel({ event }: { event: PublicEventResponse }) {
  const me = useMe();
  const location = useLocation();

  if (me.isPending) {
    return <Spin />;
  }
  if (me.isSuccess) {
    return <MemberRegistration event={event} />;
  }
  if (event.visibility === "INTERNAL") {
    return (
      <Flex vertical gap={12}>
        <Typography.Text>
          Sự kiện này chỉ dành cho thành viên CLB.
        </Typography.Text>
        <Link to="/login" state={{ from: location.pathname }}>
          <Button type="primary" block>
            Đăng nhập để đăng ký
          </Button>
        </Link>
      </Flex>
    );
  }
  return <GuestRegistration event={event} />;
}

// Lý do không đăng ký được, dựa trên các field backend trả (registrationOpen đã tính sẵn)
function ClosedNotice({ event }: { event: PublicEventResponse }) {
  if (event.status === "COMPLETED") {
    return <Alert type="info" title="Sự kiện đã kết thúc" />;
  }
  if (isFuture(event.registrationOpensAt)) {
    return (
      <Alert
        type="info"
        title={`Mở đăng ký lúc ${formatDateTime(event.registrationOpensAt)}`}
      />
    );
  }
  return (
    <Alert
      type="warning"
      title={`Đã đóng đăng ký lúc ${formatDateTime(event.registrationClosesAt)}`}
    />
  );
}

function MemberRegistration({ event }: { event: PublicEventResponse }) {
  const mine = useMyRegistrations();
  const register = useRegistrationMutation(
    () => registerAsMember(event.id),
    "Đăng ký thành công",
  );
  const cancel = useCancelRegistration();

  if (mine.isPending) {
    return <Spin />;
  }
  if (mine.isError) {
    return <Alert type="error" title={errorMessage(mine.error)} />;
  }

  // Đăng ký còn hiệu lực của mình cho sự kiện này (đăng ký đã hủy thì bỏ qua, được đăng ký lại)
  const current = mine.data.find(
    (r) => r.event.id === event.id && isActiveRegistration(r.status),
  );

  if (current) {
    const tag = REGISTRATION_STATUS_TAGS[current.status];
    return (
      <Flex vertical gap={12}>
        <div>
          <Tag color={tag.color}>{tag.label}</Tag>
          <Typography.Text type="secondary">
            lúc {formatDateTime(current.createdAt)}
          </Typography.Text>
        </div>
        {/* cancellable do backend tính bằng đúng hàm mà API hủy dùng → nút hiện ra là bấm được */}
        {current.cancellable && (
          <Popconfirm
            title="Hủy đăng ký?"
            description="Chỗ của bạn sẽ được nhường cho người khác"
            okText="Hủy đăng ký"
            okButtonProps={{ danger: true }}
            cancelText="Không"
            onConfirm={() => cancel.mutate(current.id)}
          >
            <Button danger block loading={cancel.isPending}>
              Hủy đăng ký
            </Button>
          </Popconfirm>
        )}
      </Flex>
    );
  }

  if (!event.registrationOpen) {
    return <ClosedNotice event={event} />;
  }
  return (
    <Button
      type="primary"
      size="large"
      block
      loading={register.isPending} // đang gửi thì khóa nút, đỡ bấm nhiều lần (backend vẫn chặn trùng)
      onClick={() => register.mutate(undefined)}
    >
      Đăng ký tham gia
    </Button>
  );
}

function GuestRegistration({ event }: { event: PublicEventResponse }) {
  const { message } = App.useApp();
  const location = useLocation();
  // Khách không có "sự kiện của tôi" để tải lại → dùng useMutation thẳng, đăng ký xong hiện màn hình cảm ơn
  const register = useMutation({
    mutationFn: (request: GuestRegistrationRequest) =>
      registerAsGuest(event.id, request),
    onError: (err) => message.error(errorMessage(err)),
  });

  if (register.isSuccess) {
    return (
      <Result
        status="success"
        title="Đăng ký thành công"
        subTitle={`Đã ghi nhận đăng ký cho ${register.variables.email}`}
      />
    );
  }
  if (!event.registrationOpen) {
    return <ClosedNotice event={event} />;
  }

  return (
    // rules giống @NotBlank/@Email/@Pattern bên GuestRegistrationRequest: báo lỗi ngay, khỏi đợi 400
    <Form<GuestRegistrationRequest>
      layout="vertical"
      onFinish={(values) => register.mutate(values)}
      disabled={register.isPending}
    >
      <Form.Item
        label="Họ tên"
        name="fullName"
        rules={[
          { required: true, whitespace: true, message: "Hãy nhập họ tên" },
          { max: 100, message: "Tối đa 100 ký tự" },
        ]}
      >
        <Input autoComplete="name" />
      </Form.Item>
      <Form.Item
        label="Email"
        name="email"
        rules={[
          { required: true, message: "Hãy nhập email" },
          { type: "email", message: "Email không hợp lệ" },
        ]}
      >
        <Input autoComplete="email" />
      </Form.Item>
      <Form.Item
        label="Số điện thoại"
        name="phone"
        rules={[
          { required: true, message: "Hãy nhập số điện thoại" },
          {
            // Cùng luật với backend; bỏ dấu cách trước khi so (backend cũng bỏ)
            validator: (_, value?: string) =>
              !value || /^(0|\+84)\d{9,10}$/.test(value.replace(/ /g, ""))
                ? Promise.resolve()
                : Promise.reject(new Error("Số điện thoại không hợp lệ")),
          },
        ]}
      >
        <Input autoComplete="tel" inputMode="tel" />
      </Form.Item>
      <Form.Item
        label="MSSV hoặc trường"
        name="school"
        rules={[
          {
            required: true,
            whitespace: true,
            message: "Hãy nhập MSSV hoặc tên trường",
          },
          { max: 200, message: "Tối đa 200 ký tự" },
        ]}
      >
        <Input />
      </Form.Item>

      <Button
        type="primary"
        htmlType="submit"
        size="large"
        block
        loading={register.isPending}
      >
        Đăng ký
      </Button>
      <Typography.Paragraph
        type="secondary"
        style={{ marginTop: 12, marginBottom: 0 }}
      >
        Là thành viên CLB?{" "}
        <Link to="/login" state={{ from: location.pathname }}>
          Đăng nhập
        </Link>{" "}
        để đăng ký bằng tài khoản.
      </Typography.Paragraph>
    </Form>
  );
}
