# FEVeID – Club Management System

Website quản lý CLB tổ chức sự kiện: thành viên, ban, sự kiện, tuyển CTV, điểm hoạt động & shop đổi quà, danh hiệu, ticket, thông báo.

**Functional Requirements đầy đủ: [docs/requirements.md](docs/requirements.md)** — đọc file này trước khi làm bất kỳ tính năng nào. Đây là bản chính thức; `docs/Context.docx` chỉ là bản gốc để tham khảo. Usecase diagram: `docs/UsecaseDiagram/`.

**Tiến độ & việc tiếp theo: [docs/roadmap.md](docs/roadmap.md)** — đọc khi bắt đầu phiên làm việc mới để biết đang ở giai đoạn nào; đánh dấu `[x]` khi hoàn thành một mục.

## Tóm tắt nghiệp vụ

Actor (quyền kế thừa: Department Leader, HR, BCN đều có toàn bộ quyền của Member):

| Actor | Phạm vi chính |
|---|---|
| Guest | Xem thông tin CLB, sự kiện công khai, đợt tuyển; ứng tuyển CTV; đăng ký sự kiện công khai (không cần tài khoản) |
| Member | Profile, Personal Journey, xem/wishlist/đăng ký sự kiện, Event Proposal, Ticket, Notification, điểm & shop |
| Department Leader (Ban Điều Hành) | Quản lý & đánh giá thành viên trong ban, đề xuất đổi trạng thái/chức vụ cho HR, thông báo/email theo ban |
| HR | Quản lý toàn bộ thành viên (tạo tài khoản, gán ban/chức vụ, Active/Inactive/Disqualify), tuyển CTV, ticket nhân sự, xác nhận tổ chức/hỗ trợ sự kiện, quản lý điểm & shop |
| President / BCN | Quản trị CLB, quản lý sự kiện, duyệt Event Proposal, ticket cấp cao, thông báo toàn CLB, danh hiệu, dashboard tổng |

Module (theo mục trong requirements.md): Club info (1, 2.2, 5.1) · Auth & Profile (2.1) · Member/Department (4.1, 3.1) · Journey (2.3) · Event (2.4, 5.2, 6) · Event Proposal (2.5, 5.3) · Recruitment (4.2) · Award (7) · Notification (2.7, 8) · Ticket (2.6, 9) · Dashboard (10) · Điểm & Shop (11) · Đăng ký / Check-in / Thanh toán (12).

State machine trong FR:
- Event: `DRAFT → OPEN_REGISTRATION → UPCOMING → ONGOING → COMPLETED` (+ `CANCELLED`)
- Event Proposal: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED` (+ `REVISION_REQUIRED`)
- Ticket: `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED` (+ `REJECTED`)
- Ứng viên CTV: `APPLIED → SCREENING → INTERVIEW → PASSED / REJECTED`
- Đăng ký sự kiện: `PENDING_PAYMENT → CONFIRMED → CHECKED_IN` (+ `CANCELLED`, `EXPIRED`, `NO_SHOW`)
- Đổi quà: `RESERVED → DELIVERED` (+ `CANCELLED`)

## Cấu trúc repo

```
FEVeID_BackEnd/    Spring Boot REST API
FEVeID_FrontEnd/   React (chưa khởi tạo)
docs/              requirements.md (FR chính thức), Context.docx (bản gốc), UsecaseDiagram/
```

## Backend

- Java 25, Spring Boot 4.1, PostgreSQL, Flyway, Spring Security + JWT (jjwt), Lombok, springdoc (Swagger UI: `/swagger-ui.html`).
- Chạy (trong `FEVeID_BackEnd/`): `./mvnw spring-boot:run`; test: `./mvnw test`.
- Biến môi trường bắt buộc: xem `FEVeID_BackEnd/.env.example` (`DB_PASSWORD`, `JWT_SECRET`). Không bao giờ commit secret – repo là public.
- Package theo feature: `feveid/<module>/{controller,service,service/impl,repository,entity,dto,enums}`.
- DTO dùng Java `record` + Jakarta Validation.
- Schema chỉ thay đổi qua Flyway migration mới (`src/main/resources/db/migration/V{n}__mo_ta.sql`); không sửa migration đã chạy. `ddl-auto=validate`.

## Frontend (dự kiến)

React + TypeScript + Vite, Ant Design, TanStack Query, React Router, React Hook Form + Zod; API client sinh từ OpenAPI của backend.

## Quyết định nghiệp vụ đã chốt

- Điểm hoạt động là **sổ cái** (`point_transactions`, append-only, gắn `term_id`), không lưu cột `points`. Tổ chức +4, hỗ trợ +2 (HR xác nhận), tham gia +1 (qua check-in).
- Điểm reset theo kỳ (số dư = tổng giao dịch của kỳ hiện tại); vẫn ghi nhận tổng điểm trọn đời.
- Đăng ký nhưng không đến → `NO_SHOW`, không cộng điểm, không phạt.
- Sự kiện có phí: thanh toán qua VietQR, hệ thống tự xác nhận đăng ký khi nhận được tiền. Sự kiện miễn phí: đăng ký trực tiếp.
- Check-in (để cộng điểm tham gia) tách biệt với đăng ký/thanh toán.
- Role toàn CLB (`MEMBER`, `HR`, `PRESIDENT`) khác với chức vụ trong ban (Department Leader là chức vụ trong ban).

## Quy ước Git

- Nhánh chính `master`, remote `origin`.
- Commit message: `feat: ...`, `fix: ...`, `chore: ...`, `docs: ...`, `refactor: ...`, `test: ...`.
