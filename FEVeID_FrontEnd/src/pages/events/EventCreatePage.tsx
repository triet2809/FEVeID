import { Card, Typography } from "antd";
import { useNavigate } from "react-router";
import { createEvent } from "../../events/eventApi";
import { EventInfoForm } from "../../events/EventInfoForm";
import { useEventMutation } from "../../events/eventQueries";

// Tạo xong là bản nháp chưa có lịch → chuyển sang trang chi tiết để lập lịch, thêm ảnh, rồi công bố
export function EventCreatePage() {
  const navigate = useNavigate();
  const create = useEventMutation(createEvent, "Đã tạo bản nháp");

  return (
    <>
      <Typography.Title level={3}>Tạo sự kiện</Typography.Title>
      <Card>
        <EventInfoForm
          submitText="Tạo bản nháp"
          submitting={create.isPending}
          // mutate(biến, { onSuccess riêng cho lần gọi này }): chạy SAU onSuccess chung trong useEventMutation
          onSubmit={(request) =>
            create.mutate(request, {
              onSuccess: (event) => navigate(`/app/events/${event.id}`),
            })
          }
        />
      </Card>
    </>
  );
}
