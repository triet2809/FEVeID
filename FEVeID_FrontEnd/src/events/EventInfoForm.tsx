import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
} from "antd";
import type { Dayjs } from "dayjs";
import type { EventRequest, EventResponse } from "../api/types";
import { toIso, toPickerValue } from "../lib/time";
import { VISIBILITY_LABELS } from "./eventLabels";
import { useMembers } from "./eventQueries";

// Giá trị bên trong form (khác EventRequest gửi lên): giờ là Dayjs để DatePicker dùng,
// 2 giờ mở/đóng đăng ký gộp thành 1 ô chọn khoảng → tự đảm bảo "nhập cả hai hoặc bỏ trống cả hai"
type FormValues = {
  name: string;
  description?: string;
  location?: string;
  visibility: EventRequest["visibility"];
  capacity?: number | null;
  registration?: [Dayjs, Dayjs] | null;
  ownerMemberId?: number | null;
};

// Đổi qua lại giữa DTO và giá trị form (giống mapper Entity ↔ DTO bên Java).
// "?? undefined": null → undefined, vì JSON.stringify bỏ field undefined → backend nhận null
function toFormValues(event: EventResponse | undefined): FormValues {
  if (!event) {
    return { name: "", visibility: "INTERNAL" };
  }
  const opens = toPickerValue(event.registrationOpensAt);
  const closes = toPickerValue(event.registrationClosesAt);
  return {
    name: event.name,
    description: event.description,
    location: event.location,
    visibility: event.visibility,
    capacity: event.capacity,
    registration: opens && closes ? [opens, closes] : null,
    ownerMemberId: event.owner?.id,
  };
}

function toRequest(values: FormValues): EventRequest {
  return {
    name: values.name,
    description: values.description || undefined, // ô trống "" → không gửi
    location: values.location || undefined,
    visibility: values.visibility,
    capacity: values.capacity ?? undefined,
    registrationOpensAt: toIso(values.registration?.[0]),
    registrationClosesAt: toIso(values.registration?.[1]),
    ownerMemberId: values.ownerMemberId ?? undefined,
  };
}

type EventInfoFormProps = {
  event?: EventResponse; // không truyền = form tạo mới
  submitText: string;
  submitting: boolean;
  disabled?: boolean;
  onSubmit: (request: EventRequest) => void;
};

export function EventInfoForm({
  event,
  submitText,
  submitting,
  disabled,
  onSubmit,
}: EventInfoFormProps) {
  const members = useMembers();

  return (
    // initialValues của antd Form chỉ được đọc LẦN ĐẦU, sau đó form tự giữ giá trị người dùng gõ.
    // key: chuyển sang sự kiện khác → React tạo form mới → đọc lại initialValues
    <Form<FormValues>
      key={event?.id ?? "new"}
      layout="vertical"
      initialValues={toFormValues(event)}
      disabled={disabled}
      onFinish={(values) => onSubmit(toRequest(values))}
      style={{ maxWidth: 720 }}
    >
      {/* rules giống @NotBlank/@Size bên EventRequest: báo lỗi ngay, khỏi đợi backend trả 400 */}
      <Form.Item
        label="Tên sự kiện"
        name="name"
        rules={[
          { required: true, whitespace: true, message: "Hãy nhập tên sự kiện" },
          { max: 200, message: "Tối đa 200 ký tự" },
        ]}
      >
        <Input />
      </Form.Item>

      <Form.Item
        label="Mô tả"
        name="description"
        rules={[{ max: 5000, message: "Tối đa 5000 ký tự" }]}
      >
        <Input.TextArea
          autoSize={{ minRows: 3, maxRows: 10 }}
          showCount
          maxLength={5000}
        />
      </Form.Item>

      <Form.Item
        label="Địa điểm"
        name="location"
        rules={[{ max: 300, message: "Tối đa 300 ký tự" }]}
      >
        <Input />
      </Form.Item>

      <Form.Item
        label="Ai được đăng ký"
        name="visibility"
        rules={[{ required: true }]}
      >
        <Radio.Group
          options={(["INTERNAL", "PUBLIC"] as const).map((value) => ({
            value,
            label:
              value === "INTERNAL"
                ? "Nội bộ (thành viên CLB)"
                : `${VISIBILITY_LABELS.PUBLIC} (cả người ngoài)`,
          }))}
        />
      </Form.Item>

      <Form.Item
        label="Số chỗ"
        name="capacity"
        extra="Để trống nếu không giới hạn"
      >
        <InputNumber min={1} precision={0} style={{ width: 200 }} />
      </Form.Item>

      <Form.Item
        label="Thời gian mở đăng ký (giờ VN)"
        name="registration"
        extra="Bắt buộc trước khi công bố. Giờ đóng không được sau giờ kết thúc sự kiện"
      >
        <DatePicker.RangePicker
          showTime={{ format: "HH:mm" }}
          format="DD/MM/YYYY HH:mm"
          placeholder={["Mở đăng ký", "Đóng đăng ký"]}
        />
      </Form.Item>

      <Form.Item
        label="Người phụ trách"
        name="ownerMemberId"
        extra="Có thể gán sau, khi đã chọn được team"
      >
        <Select
          allowClear
          showSearch={{ optionFilterProp: "label" }} // gõ tên để lọc
          loading={members.isPending}
          placeholder="Chọn thành viên"
          options={members.data?.map((member) => ({
            value: member.id,
            label: `${member.fullName} (${member.email})`,
          }))}
          style={{ maxWidth: 400 }}
        />
      </Form.Item>

      <Button type="primary" htmlType="submit" loading={submitting}>
        {submitText}
      </Button>
    </Form>
  );
}
