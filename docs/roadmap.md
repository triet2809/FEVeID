# Lộ trình phát triển

Nguyên tắc: làm theo lát dọc (migration → entity → API → test → màn hình) cho từng tính năng; mỗi giai đoạn phải ra được thứ CLB dùng thật được.

## Giai đoạn 0 – Nền móng (đang làm)

### Đã xong
- [x] Monorepo + git + GitHub, `.gitignore`, gỡ file IDE khỏi git
- [x] JWT secret chuyển sang biến môi trường `JWT_SECRET`
- [x] FR chuyển sang `docs/requirements.md`, `CLAUDE.md`

### Lỗi trong code hiện tại cần sửa (phát hiện 09/10/2026)
- [ ] **Account DISABLED/LOCKED vẫn login được** – `auth/service/impl/UserDetailsServiceImpl.java` trả `new User(email, hash, authorities)` nên enabled/nonLocked luôn true. JWT filter cũng không kiểm tra lại trạng thái.
- [ ] **Thiếu phân quyền** – `AccountController`: mọi user đã đăng nhập đều tạo được tài khoản (`POST /api/accounts/register`) và xem toàn bộ tài khoản. Cần `@EnableMethodSecurity` + `@PreAuthorize("hasRole('HR')")`; `GET /{id}` chỉ cho chính chủ hoặc HR/BCN.
- [ ] **Exception handler** – `GlobalExceptionHandler` bắt mọi `RuntimeException` → 400 kèm `ex.getMessage()`: sai mật khẩu phải là 401, không tìm thấy là 404, trùng email là 409, lỗi lạ là 500 với message chung (không lộ chi tiết). Tạo exception riêng (`NotFoundException`, `ConflictException`, …).
- [ ] **HR đặt mật khẩu cho member** – nên gửi link đặt mật khẩu qua email, hoặc bắt đổi mật khẩu ở lần đăng nhập đầu.
- [ ] Dọn dẹp: import thừa `AbstractPersistable_.id` trong `AccountController`; `email` dài 50 ở entity nhưng 255 ở DB; package `comon` → `common`; `SecurityConfig` chuyển ra package config chung.

### Mô hình dữ liệu nền
- [ ] Tách `accounts` (đăng nhập) và `members` (hồ sơ CLB, member_status: CTV/ACTIVE/INACTIVE/DISQUALIFIED)
- [ ] `departments` + `department_memberships(member, department, position, start_date, end_date)` – Department Leader là chức vụ trong ban
- [ ] `terms` (kỳ hoạt động, thời gian mở shop)
- [ ] Module upload file dùng chung (Cloudinary / Cloudflare R2)
- [x] Test tích hợp với Testcontainers
- [ ] Khởi tạo frontend React (login + layout theo role)

## Giai đoạn 1 – Sự kiện miễn phí trọn vòng
CRUD sự kiện, trang sự kiện công khai, đăng ký (nội bộ + khách), check-in QR, Finalize sự kiện (HR chốt tổ chức/hỗ trợ), sổ cái điểm.

## Giai đoạn 2 – Shop & tổng kết kỳ
Shop đổi quà, phát quà ở buổi tổng kết, Personal Journey, Dashboard cơ bản, thông báo trong app + email.

## Giai đoạn 3 – Sự kiện có phí
VietQR + webhook biến động số dư (SePay/Casso/PayOS – cần kiểm tra điều kiện), giữ chỗ có hạn, đối soát thủ công.

## Giai đoạn 4+
Event Proposal, tuyển CTV, Awards, Ticket, tính năng riêng của Department Leader, thống kê nâng cao, CMS thông tin CLB (giai đoạn đầu hardcode ở frontend).

## Ghi chú thiết kế đã thống nhất
- Trạng thái sự kiện nên chỉ lưu các bước do người quyết định (`DRAFT`, `PUBLISHED`, `COMPLETED`, `FINALIZED`, `CANCELLED`); "đang mở đăng ký / sắp diễn ra / đang diễn ra" suy ra từ thời gian. (Khác với FR mục 5.2 – cần chốt khi làm module Event.)
- Đổi quà: khóa ví (`SELECT ... FOR UPDATE`) + trừ tồn kho atomic để chống tiêu điểm hai lần / vượt tồn kho.
- Giới hạn số lượng đăng ký: `UPDATE ... WHERE registered < capacity` (atomic).
- Check-in member: QR xoay vòng (HMAC theo khung thời gian ~30s); khách: vé QR cá nhân qua email; luôn có điểm danh thủ công dự phòng.
- Chia sẻ link sự kiện lên Facebook: backend có endpoint trả thẻ Open Graph rồi redirect sang trang React.
