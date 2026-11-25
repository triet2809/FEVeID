import { api } from "../api/client";
import type {
  EventRegistrationResponse,
  EventStatus,
  GuestRegistrationRequest,
  MyRegistrationResponse,
  PublicEventResponse,
  RegistrationResponse,
} from "../api/types";

// Mỗi hàm = một endpoint (PublicEventController, RegistrationController, GuestRegistrationController,
// EventRegistrationController bên backend)

export type PublicEventFilter = {
  status?: EventStatus;
  q?: string;
};

// URLSearchParams ≈ @RequestParam: { status: "UPCOMING", q: "nhạc" } → "?status=UPCOMING&q=nh%E1%BA%A1c"
export function fetchPublicEvents(
  filter: PublicEventFilter,
): Promise<PublicEventResponse[]> {
  const params = new URLSearchParams();
  if (filter.status) params.set("status", filter.status);
  if (filter.q?.trim()) params.set("q", filter.q.trim());
  const query = params.toString();
  return api.get<PublicEventResponse[]>(
    `/api/public/events${query ? `?${query}` : ""}`,
  );
}

export function fetchPublicEvent(id: number): Promise<PublicEventResponse> {
  return api.get<PublicEventResponse>(`/api/public/events/${id}`);
}

// Member: backend lấy người đăng ký từ cookie đăng nhập, chỉ cần gửi eventId
export function registerAsMember(
  eventId: number,
): Promise<RegistrationResponse> {
  return api.post<RegistrationResponse>("/api/registrations", { eventId });
}

export function registerAsGuest(
  eventId: number,
  request: GuestRegistrationRequest,
): Promise<RegistrationResponse> {
  return api.post<RegistrationResponse>(
    `/api/public/events/${eventId}/registrations`,
    request,
  );
}

export function cancelRegistration(
  registrationId: number,
): Promise<RegistrationResponse> {
  return api.post<RegistrationResponse>(
    `/api/registrations/${registrationId}/cancel`,
  );
}

export function fetchMyRegistrations(): Promise<MyRegistrationResponse[]> {
  return api.get<MyRegistrationResponse[]>("/api/registrations/me");
}

// Chỉ HR/BCN (trang quản lý sự kiện)
export function fetchEventRegistrations(
  eventId: number,
): Promise<EventRegistrationResponse[]> {
  return api.get<EventRegistrationResponse[]>(
    `/api/events/${eventId}/registrations`,
  );
}
