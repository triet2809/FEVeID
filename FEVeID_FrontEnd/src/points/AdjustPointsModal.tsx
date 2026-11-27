import { Form, Input, InputNumber, Modal, Select } from "antd";
import type { PointAdjustmentRequest } from "../api/types";
import { useTerms } from "../terms/termQueries";
import { useAdjustPoints } from "./pointQueries";

type Props = {
  memberId: number;
  memberName: string;
  open: boolean;
  onClose: () => void;
};

// HR/BCN cộng/trừ điểm tay (requirements 11.4). Luật kiểm tra giống PointAdjustmentRequest bên backend;
// backend vẫn kiểm tra lại (số dư âm, không có kỳ hiện tại) và lỗi đó hiện bằng message
export function AdjustPointsModal({
  memberId,
  memberName,
  open,
  onClose,
}: Props) {
  const [form] = Form.useForm<PointAdjustmentRequest>();
  const terms = useTerms();
  const adjust = useAdjustPoints(memberId, () => {
    form.resetFields();
    onClose();
  });

  return (
    <Modal
      title={`Điều chỉnh điểm: ${memberName}`}
      open={open}
      okText="Lưu"
      cancelText="Đóng"
      confirmLoading={adjust.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => adjust.mutate(values)}
      >
        <Form.Item
          name="termId"
          label="Kỳ"
          extra="Để trống = kỳ hiện tại. Chọn kỳ cũ khi sửa điểm theo khiếu nại"
        >
          <Select
            allowClear
            placeholder="Kỳ hiện tại"
            loading={terms.isPending}
            options={terms.data?.map((t) => ({ value: t.id, label: t.name }))}
          />
        </Form.Item>
        <Form.Item
          name="amount"
          label="Số điểm"
          extra="Số dương để cộng, số âm để trừ (vd -3)"
          rules={[
            { required: true, message: "Nhập số điểm" },
            {
              validator: (_, value) =>
                value === 0
                  ? Promise.reject(new Error("Số điểm phải khác 0"))
                  : Promise.resolve(),
            },
          ]}
        >
          <InputNumber
            min={-1000}
            max={1000}
            precision={0}
            style={{ width: "100%" }}
          />
        </Form.Item>
        <Form.Item
          name="reason"
          label="Lý do"
          rules={[
            {
              required: true,
              whitespace: true,
              message: "Bắt buộc ghi lý do điều chỉnh",
            },
            { max: 500 },
          ]}
        >
          <Input.TextArea
            rows={3}
            placeholder="vd: Bù điểm hỗ trợ sự kiện Hội thao bị sót"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
