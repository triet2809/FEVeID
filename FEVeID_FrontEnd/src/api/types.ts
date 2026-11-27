import type { components } from "./schema";

// Đặt tên ngắn cho các kiểu sinh từ OpenAPI (schema.d.ts: KHÔNG sửa tay, chạy lại `npm run gen:api`).
// Backend đổi DTO → sinh lại → chỗ nào dùng sai field sẽ báo đỏ khi build (giống đổi record bên Java).
type Schemas = components["schemas"];

export type MemberResponse = Schemas["MemberResponse"];
export type AccountRole = MemberResponse["role"]; // "PRESIDENT" | "HR" | "MEMBER"
export type LoginRequest = Schemas["LoginRequest"];

export type EventResponse = Schemas["EventResponse"];
export type EventRequest = Schemas["EventRequest"];
export type EventStatus = EventResponse["status"]; // DRAFT | UPCOMING | ONGOING | COMPLETED | CANCELLED
export type EventVisibility = EventResponse["visibility"];
export type PhaseResponse = Schemas["PhaseResponse"];
export type PhaseRequest = Schemas["PhaseRequest"];
export type PhaseKind = PhaseResponse["kind"]; // UPCOMING | ONGOING | COMPLETED

// Trang sự kiện công khai (khách + member) và đăng ký (Bài 11)
export type PublicEventResponse = Schemas["PublicEventResponse"];
export type RegistrationResponse = Schemas["RegistrationResponse"];
export type RegistrationStatus = RegistrationResponse["status"]; // CONFIRMED | CANCELLED | ...
export type GuestRegistrationRequest = Schemas["GuestRegistrationRequest"];
export type MyRegistrationResponse = Schemas["MyRegistrationResponse"];
export type EventRegistrationResponse = Schemas["EventRegistrationResponse"];

// Check-in (Bài 12)
export type CheckInCodeResponse = Schemas["CheckInCodeResponse"];
export type CheckInRequest = Schemas["CheckInRequest"];
export type CheckInResponse = Schemas["CheckInResponse"];
