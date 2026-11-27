import { api } from "../api/client";
import type {
  EventRequest,
  EventResponse,
  EventStaffResponse,
  FinalizationResponse,
  FinalizeEventRequest,
  MemberResponse,
  PhaseRequest,
  StaffAssignmentRequest,
} from "../api/types";

// Mỗi hàm = một endpoint của EventController (giống một method trong Feign client / RestTemplate bên Java)

export function fetchEvents(): Promise<EventResponse[]> {
  return api.get<EventResponse[]>("/api/events");
}

export function fetchEvent(id: number): Promise<EventResponse> {
  return api.get<EventResponse>(`/api/events/${id}`);
}

export function createEvent(request: EventRequest): Promise<EventResponse> {
  return api.post<EventResponse>("/api/events", request);
}

export function updateEvent(
  id: number,
  request: EventRequest,
): Promise<EventResponse> {
  return api.put<EventResponse>(`/api/events/${id}`, request);
}

// Thay CẢ lịch (backend xóa lịch cũ, lưu lịch mới) → id các giai đoạn đổi sau mỗi lần lưu
export function replacePhases(
  id: number,
  phases: PhaseRequest[],
): Promise<EventResponse> {
  return api.put<EventResponse>(`/api/events/${id}/phases`, { phases });
}

export function startPhaseNow(
  id: number,
  phaseId: number,
): Promise<EventResponse> {
  return api.post<EventResponse>(
    `/api/events/${id}/phases/${phaseId}/start-now`,
  );
}

export function publishEvent(id: number): Promise<EventResponse> {
  return api.post<EventResponse>(`/api/events/${id}/publish`);
}

export function cancelEvent(id: number): Promise<EventResponse> {
  return api.post<EventResponse>(`/api/events/${id}/cancel`);
}

export function deleteEvent(id: number): Promise<void> {
  return api.delete<void>(`/api/events/${id}`);
}

// multipart/form-data, part tên "file" = @RequestParam("file") MultipartFile bên backend
export function uploadBanner(id: number, file: File): Promise<EventResponse> {
  const form = new FormData();
  form.append("file", file);
  return api.upload<EventResponse>(`/api/events/${id}/banner`, form);
}

export function deleteBanner(id: number): Promise<EventResponse> {
  return api.delete<EventResponse>(`/api/events/${id}/banner`);
}

// Để chọn người phụ trách. GET /api/members cũng chỉ HR/BCN gọi được, trùng quyền với trang sự kiện
export function fetchMembers(): Promise<MemberResponse[]> {
  return api.get<MemberResponse[]>("/api/members");
}

// ---------- Team sự kiện (EventStaffController) ----------

export function fetchEventStaff(id: number): Promise<EventStaffResponse[]> {
  return api.get<EventStaffResponse[]>(`/api/events/${id}/staff`);
}

// Thay CẢ team: ai không có trong list là bị gỡ (giống PUT lịch giai đoạn)
export function replaceEventStaff(
  id: number,
  staff: StaffAssignmentRequest[],
): Promise<EventStaffResponse[]> {
  return api.put<EventStaffResponse[]>(`/api/events/${id}/staff`, { staff });
}

// ---------- Chốt sự kiện (EventFinalizationController) ----------

// Chưa chốt: bản xem trước. Đã chốt: điểm đã ghi trong sổ cái
export function fetchFinalization(id: number): Promise<FinalizationResponse> {
  return api.get<FinalizationResponse>(`/api/events/${id}/finalization`);
}

export function finalizeEvent(
  id: number,
  request: FinalizeEventRequest,
): Promise<FinalizationResponse> {
  return api.post<FinalizationResponse>(
    `/api/events/${id}/finalization`,
    request,
  );
}
