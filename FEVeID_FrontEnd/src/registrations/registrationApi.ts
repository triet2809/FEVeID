import { api } from "../api/client";
import type {
  CheckInCodeResponse,
  CheckInResponse,
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

// ---------- Check-in (Bài 12, CheckInController + EventRegistrationController) ----------

// HR/BCN: mã QR hiện tại + số người đã check-in. 409 nếu sự kiện chưa/không còn ONGOING
export function fetchCheckInCode(
  eventId: number,
): Promise<CheckInCodeResponse> {
  return api.get<CheckInCodeResponse>(`/api/events/${eventId}/check-in-code`);
}

// Member: gửi lại nguyên eventId + code đọc được từ link trong QR
export function checkIn(
  eventId: number,
  code: string,
): Promise<CheckInResponse> {
  return api.post<CheckInResponse>("/api/check-ins", { eventId, code });
}

// HR/BCN: check-in tay một đăng ký (member hoặc khách)
export function checkInManually(
  eventId: number,
  registrationId: number,
): Promise<EventRegistrationResponse> {
  return api.post<EventRegistrationResponse>(
    `/api/events/${eventId}/registrations/${registrationId}/check-in`,
  );
}
