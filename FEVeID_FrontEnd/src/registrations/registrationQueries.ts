import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { errorMessage } from "../api/client";
import type { RegistrationResponse } from "../api/types";
import { useMe } from "../auth/useMe";
import {
  cancelRegistration,
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

export function useEventRegistrations(eventId: number) {
  return useQuery({
    queryKey: eventRegistrationsKey(eventId),
    queryFn: () => fetchEventRegistrations(eventId),
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
