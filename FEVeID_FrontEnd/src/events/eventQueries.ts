import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { errorMessage } from "../api/client";
import type {
  EventResponse,
  FinalizeEventRequest,
  StaffAssignmentRequest,
} from "../api/types";
import { POINTS_KEY } from "../points/pointQueries";
import {
  eventRegistrationsKey,
  MY_REGISTRATIONS_KEY,
} from "../registrations/registrationQueries";
import {
  fetchEvent,
  fetchEventStaff,
  fetchEvents,
  fetchFinalization,
  fetchMembers,
  finalizeEvent,
  replaceEventStaff,
} from "./eventApi";

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

// ---------- Team sự kiện (Bài 13.1) ----------

export const eventStaffKey = (id: number) => ["event-staff", id] as const;
export const finalizationKey = (id: number) => ["finalization", id] as const;

export function useEventStaff(id: number) {
  return useQuery({
    queryKey: eventStaffKey(id),
    queryFn: () => fetchEventStaff(id),
  });
}

// Lưu team xong: ghi thẳng danh sách mới vào cache + làm mới màn chốt (danh sách tick sẵn lấy từ team)
export function useReplaceEventStaff(id: number) {
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  return useMutation({
    mutationFn: (staff: StaffAssignmentRequest[]) =>
      replaceEventStaff(id, staff),
    onSuccess: (staff) => {
      queryClient.setQueryData(eventStaffKey(id), staff);
      queryClient.invalidateQueries({ queryKey: finalizationKey(id) });
      message.success("Đã lưu team");
    },
    onError: (err) => message.error(errorMessage(err)),
  });
}

// ---------- Chốt sự kiện (Bài 13.2) ----------

export function useFinalization(id: number) {
  return useQuery({
    queryKey: finalizationKey(id),
    queryFn: () => fetchFinalization(id),
  });
}

// Chốt xong, nhiều thứ đổi cùng lúc ở backend (giống 1 @Transactional sửa nhiều bảng) → làm mới từng cache liên quan:
//   sự kiện (finalizedAt), danh sách đăng ký (CONFIRMED → NO_SHOW), "sự kiện của tôi", điểm của mọi người
export function useFinalizeEvent(id: number) {
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  return useMutation({
    mutationFn: (request: FinalizeEventRequest) => finalizeEvent(id, request),
    onSuccess: (result) => {
      queryClient.setQueryData(finalizationKey(id), result);
      queryClient.invalidateQueries({ queryKey: eventKey(id) });
      queryClient.invalidateQueries({ queryKey: EVENTS_KEY, exact: true });
      queryClient.invalidateQueries({ queryKey: eventRegistrationsKey(id) });
      queryClient.invalidateQueries({ queryKey: MY_REGISTRATIONS_KEY });
      queryClient.invalidateQueries({ queryKey: POINTS_KEY });
      message.success(`Đã chốt sự kiện, phát ${result.totalPoints} điểm`);
    },
    onError: (err) => message.error(errorMessage(err)),
  });
}
