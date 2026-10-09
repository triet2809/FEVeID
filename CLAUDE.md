# FEVeID – Club Management System

Website quản lý CLB tổ chức sự kiện: thành viên, ban, sự kiện, tuyển CTV, điểm hoạt động & shop đổi quà, danh hiệu, ticket, thông báo.

## Cấu trúc repo

```
FEVeID_BackEnd/    Spring Boot REST API
FEVeID_FrontEnd/   React (chưa khởi tạo)
docs/              Context.docx (Functional Requirements), UsecaseDiagram/
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
