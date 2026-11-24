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
