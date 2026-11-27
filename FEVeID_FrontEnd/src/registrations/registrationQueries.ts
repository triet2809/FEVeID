import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { errorMessage } from "../api/client";
import type { CheckInCodeResponse, RegistrationResponse } from "../api/types";
import { useMe } from "../auth/useMe";
import {
  cancelRegistration,
  checkInManually,
  fetchCheckInCode,
  fetchEventRegistrations,
  fetchMyRegistrations,
  fetchPublicEvent,
  fetchPublicEvents,
  type PublicEventFilter,
} from "./registrationApi";

// Khóa cache. Trang công khai trả nội dung KHÁC NHAU cho khách và member (member thấy cả sự kiện nội bộ),
// nên đưa signedIn vào khóa: đăng nhập xong quay lại trang thì không dùng nhầm bản đã tải lúc còn là khách
export const PUBLIC_EVENTS_KEY = ["public-events"] as const;
export const MY_REGISTRATIONS_KEY = ["my-registrations"] as const;
export const eventRegistrationsKey = (eventId: number) =>
  ["event-registrations", eventId] as const;
export const checkInCodeKey = (eventId: number) =>
  ["check-in-code", eventId] as const;

// Khách: /me trả 401 → isError → signedIn = false. Đang chờ /me thì chưa biết → tạm chưa gọi API (enabled)
function useSignedIn() {
  const me = useMe();
  return { signedIn: me.isSuccess, known: !me.isPending };
}

export function usePublicEvents(filter: PublicEventFilter) {
  const { signedIn, known } = useSignedIn();
  return useQuery({
    queryKey: [...PUBLIC_EVENTS_KEY, "list", signedIn, filter],
    queryFn: () => fetchPublicEvents(filter),
    enabled: known,
  });
}

export function usePublicEvent(id: number) {
  const { signedIn, known } = useSignedIn();
  return useQuery({
    queryKey: [...PUBLIC_EVENTS_KEY, id, signedIn],
    queryFn: () => fetchPublicEvent(id),
    enabled: known,
  });
}

// enabled: false với khách (không có "sự kiện của tôi", gọi sẽ bị 401)
export function useMyRegistrations(enabled = true) {
  return useQuery({
    queryKey: MY_REGISTRATIONS_KEY,
    queryFn: fetchMyRegistrations,
    enabled,
  });
}

// live = sự kiện đang diễn ra: tự tải lại mỗi 10 giây để HR thấy người vừa check-in (gần như thời gian thực)
export function useEventRegistrations(eventId: number, live = false) {
  return useQuery({
    queryKey: eventRegistrationsKey(eventId),
    queryFn: () => fetchEventRegistrations(eventId),
    refetchInterval: live ? 10_000 : false,
  });
}

// Màn hình chiếu QR. refetchInterval nhận một hàm: TanStack Query hỏi lại sau mỗi lần tải xong
// "bao lâu nữa thì gọi tiếp?" (giống @Scheduled nhưng tự tính lại thời gian chờ mỗi lần):
//   - có mã: gọi lại ngay khi mã hết hạn (expiresAt), nhưng không quá 5 giây để số người check-in luôn mới
//   - lỗi (409 chưa tới giờ / đã kết thúc): 5 giây hỏi lại một lần, tới giờ là màn hình tự hiện mã
export function useCheckInCode(eventId: number) {
  return useQuery({
    queryKey: checkInCodeKey(eventId),
    queryFn: () => fetchCheckInCode(eventId),
    refetchInterval: (query) => untilNextRefresh(query.state.data),
    refetchIntervalInBackground: true, // màn hình chiếu có thể đang ở tab không được chọn
    staleTime: 0,
  });
}

function untilNextRefresh(data: CheckInCodeResponse | undefined): number {
  if (!data) return 5_000;
  // +300ms: chắc chắn server đã sang ô 30 giây mới khi gọi lại (đồng hồ 2 máy lệch nhau một chút)
  const untilExpiry = new Date(data.expiresAt).getTime() - Date.now() + 300;
  return Math.min(5_000, Math.max(1_000, untilExpiry));
}

export function useManualCheckIn(eventId: number) {
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  return useMutation({
    mutationFn: (registrationId: number) =>
      checkInManually(eventId, registrationId),
    onSuccess: (registration) => {
      queryClient.invalidateQueries({
        queryKey: eventRegistrationsKey(eventId),
      });
      queryClient.invalidateQueries({ queryKey: checkInCodeKey(eventId) });
      message.success(`Đã check-in: ${registration.fullName}`);
    },
    // vd 409 "Chỉ check-in được khi sự kiện đang diễn ra"
    onError: (err) => message.error(errorMessage(err)),
  });
}

// Đăng ký / hủy xong: danh sách "của tôi" và danh sách đăng ký của sự kiện (trang HR) đều đã cũ → tải lại
export function useRegistrationMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<RegistrationResponse>,
  successMessage: string,
) {
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  return useMutation({
    mutationFn,
    onSuccess: (registration) => {
      queryClient.invalidateQueries({ queryKey: MY_REGISTRATIONS_KEY });
      queryClient.invalidateQueries({
        queryKey: eventRegistrationsKey(registration.eventId),
      });
      message.success(successMessage);
    },
    // vd 409 "Sự kiện đã hết chỗ", "Bạn đã đăng ký sự kiện này rồi"
    onError: (err) => message.error(errorMessage(err)),
  });
}

export function useCancelRegistration() {
  return useRegistrationMutation(
    (registrationId: number) => cancelRegistration(registrationId),
    "Đã hủy đăng ký",
  );
}
