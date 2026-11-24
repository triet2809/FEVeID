import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { errorMessage } from "../api/client";
import type { EventResponse } from "../api/types";
import { fetchEvent, fetchEvents, fetchMembers } from "./eventApi";

// Khóa cache (giống tên cache trong @Cacheable("events")). Danh sách và từng sự kiện là 2 khóa khác nhau:
//   ["events"]      → danh sách
//   ["events", 12]  → sự kiện id 12
// invalidate ["events"] thì xóa CẢ HAI (khóa nào bắt đầu bằng "events" cũng bị đánh dấu cũ)
export const EVENTS_KEY = ["events"] as const;
export const eventKey = (id: number) => ["events", id] as const;

export function useEvents() {
  return useQuery({ queryKey: EVENTS_KEY, queryFn: fetchEvents });
}

export function useEvent(id: number) {
  return useQuery({ queryKey: eventKey(id), queryFn: () => fetchEvent(id) });
}

export function useMembers() {
  return useQuery({ queryKey: ["members"], queryFn: fetchMembers });
}

// Mọi thao tác sửa một sự kiện (lưu thông tin, lưu lịch, công bố, hủy, upload ảnh...) đều trả về EventResponse mới.
// Dùng chung một cách xử lý (giống một @ControllerAdvice nhỏ cho frontend):
//   - thành công → ghi thẳng bản mới vào cache của sự kiện (khỏi gọi GET lại) + đánh dấu danh sách cũ + báo xanh
//   - lỗi        → hiện message của backend (vd "Giờ đóng đăng ký không được sau giờ kết thúc sự kiện")
export function useEventMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<EventResponse>,
  successMessage: string,
) {
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  return useMutation({
    mutationFn,
    onSuccess: (event) => {
      queryClient.setQueryData(eventKey(event.id), event);
      queryClient.invalidateQueries({ queryKey: EVENTS_KEY, exact: true }); // exact: chỉ danh sách, không đụng bản vừa ghi
      message.success(successMessage);
    },
    onError: (err) => message.error(errorMessage(err)),
  });
}
