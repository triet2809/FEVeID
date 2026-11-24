import { DeleteOutlined, UploadOutlined } from "@ant-design/icons";
import {
  App,
  Button,
  Empty,
  Flex,
  Image,
  Popconfirm,
  Typography,
  Upload,
} from "antd";
import type { EventResponse } from "../api/types";
import { deleteBanner, uploadBanner } from "./eventApi";
import { bannerThumb, isLocked } from "./eventLabels";
import { useEventMutation } from "./eventQueries";

const MAX_BYTES = 10 * 1024 * 1024; // = ImageValidator.MAX_BYTES bên backend
const ACCEPT = "image/jpeg,image/png,image/webp";

export function BannerSection({ event }: { event: EventResponse }) {
  const { message } = App.useApp();
  const locked = isLocked(event);
  const upload = useEventMutation(
    (file: File) => uploadBanner(event.id, file),
    "Đã cập nhật ảnh banner",
  );
  const remove = useEventMutation(
    () => deleteBanner(event.id),
    "Đã xóa ảnh banner",
  );

  // Kiểm tra sớm ở trình duyệt cho người dùng đỡ chờ. Đây chỉ là tiện lợi: đổi đuôi file là qua,
  // nên backend vẫn tự kiểm tra magic bytes (ImageValidator). Riêng file quá lớn thì backend có thể
  // cắt kết nối giữa chừng (không kịp trả 413) → chặn ở đây để người dùng thấy câu báo lỗi rõ ràng
  function handleSelect(file: File): false {
    if (!ACCEPT.split(",").includes(file.type)) {
      message.error("Chỉ nhận ảnh JPG, PNG hoặc WebP");
    } else if (file.size > MAX_BYTES) {
      message.error("Ảnh tối đa 10MB");
    } else {
      upload.mutate(file);
    }
    return false; // false = antd KHÔNG tự gửi file; mình tự gửi qua api.upload (để có cookie + CSRF)
  }

  return (
    <Flex vertical gap={16}>
      {event.bannerUrl ? (
        // src: bản thu nhỏ cho nhẹ; preview (bấm vào ảnh): bản lớn hơn
        <Image
          src={bannerThumb(event.bannerUrl, 800)}
          preview={{ src: bannerThumb(event.bannerUrl, 1920) }}
          alt={`Banner ${event.name}`}
          style={{ maxWidth: "100%", width: 800, borderRadius: 8 }}
        />
      ) : (
        <Empty description="Chưa có ảnh banner" />
      )}

      <Typography.Text type="secondary">
        JPG, PNG hoặc WebP, tối đa 10MB. Nên dùng ảnh ngang (16:9).
      </Typography.Text>

      <Flex gap={8} wrap>
        <Upload
          accept={ACCEPT}
          showUploadList={false}
          beforeUpload={handleSelect}
          disabled={locked}
        >
          <Button
            icon={<UploadOutlined />}
            loading={upload.isPending}
            disabled={locked}
          >
            {event.bannerUrl ? "Đổi ảnh" : "Tải ảnh lên"}
          </Button>
        </Upload>
        {event.bannerUrl && (
          <Popconfirm
            title="Xóa ảnh banner?"
            cancelText="Không"
            onConfirm={() => remove.mutate(undefined)}
            disabled={locked}
          >
            <Button
              danger
              icon={<DeleteOutlined />}
              loading={remove.isPending}
              disabled={locked}
            >
              Xóa ảnh
            </Button>
          </Popconfirm>
        )}
      </Flex>
    </Flex>
  );
}
