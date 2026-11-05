# Bàn giao giữa các phiên làm việc

File này dành cho Claude (và chính mình) khi chuyển máy hoặc mở phiên mới. Chủ dự án làm việc trên 2 máy: **laptop Windows** (ở công ty) và **Mac mini M4** (ở nhà). Bộ nhớ của Claude chỉ nằm trên từng máy, nên **mọi thứ cần nhớ giữa các phiên phải nằm trong file này**.

## Quy trình đầu phiên / cuối phiên

**Đầu phiên** (Claude tự làm, chỉ đọc):
1. `git fetch --all --prune`
2. `git branch -r --sort=-committerdate | head -5`: nhánh có commit mới nhất thường là việc đang làm dở.
3. Đọc mục **Trạng thái hiện tại** bên dưới **trên nhánh đó** (`git show origin/<nhánh>:docs/handoff.md`), vì bản trên `master` có thể cũ hơn.
4. So với `git status` của máy hiện tại; nếu nhánh local lệch remote thì báo người dùng trước khi làm tiếp.

**Cuối phiên** (trước khi rời máy):
1. Claude cập nhật mục **Trạng thái hiện tại** (đang ở bài nào, bước nào, việc dở dang, bước tiếp theo).
2. Commit cả việc dở dang lên nhánh đang làm (được phép dùng commit `chore: wip ...`; squash merge sẽ gộp lại sau) rồi `git push`.
3. Không để thay đổi chỉ nằm ở local, nếu không máy kia sẽ không thấy.

## Cách làm việc (mentor mode)

Chủ dự án là sinh viên năm 3, đây là dự án thật đầu tiên. Mục tiêu là **học bằng cách tự làm**, Claude hướng dẫn chứ không làm thay.

- Trả lời bằng **tiếng Việt**.
- Mỗi lần giao một khối việc vừa đủ lớn: vì sao cần làm, làm gì, lệnh để kiểm tra, các lỗi hay gặp.
- **Boilerplate** (entity, DTO, repository, migration theo mẫu có sẵn): đưa code mẫu luôn để tự gõ và hiểu.
- **Phần đáng học** (logic nghiệp vụ, test, chỗ dễ sai): đưa khung + TODO + gợi ý trước; nếu kẹt khoảng 15 phút thì đưa code mẫu.
- Trước khi chạy test: yêu cầu **đoán trước** kết quả (xanh/đỏ, status code); sau đó giải thích kết quả thật.
- Claude **không sửa code/config của dự án** trừ khi được nhờ cụ thể. Sửa docs, chạy lệnh chỉ đọc để chẩn đoán thì được.
- Luôn **kiểm tra trạng thái git/GitHub thật** trước khi coi một bước là xong.
- Hướng dẫn git: đưa cả thao tác Sourcetree lẫn lệnh git; nói rõ lệnh chạy trong shell nào.

### Quy trình Git / PR
- `master` được bảo vệ (ruleset "protect master"): chỉ merge qua PR, **squash only**, bắt buộc check CI `build` xanh. Không push thẳng lên `master`.
- Mỗi PR một thay đổi. Tạo nhánh mới từ `origin/master` mới nhất (không tạo từ nhánh đã squash merge).
- PR `fix/...`, `feat/...`: Claude review trước khi merge. PR `refactor/...`, `docs/...`, `chore/...`: tự merge khi CI xanh.
- Sau squash merge: sửa tiêu đề squash cho đúng nội dung, xóa nhánh (`git branch -D`), cập nhật master local.

## Lộ trình bài học

| Bài | Nội dung | Trạng thái |
|---|---|---|
| 1 | Docker Compose cho Postgres local | ✅ |
| 2 | Testcontainers | ✅ |
| 3 | GitHub Actions CI | ✅ |
| 4 | Branch protection | ✅ |
| 5 | Sửa lỗi nền móng theo hướng test-first (5.1 login DISABLED/LOCKED, 5.2 exception handler, 5.3 phân quyền, 5.4 dọn dẹp) | ✅ PR #4, #5, #7, #8, #9 |
| 6 | Tách `accounts` / `members` | ⏳ đang làm |
| 7 | `departments` + `department_memberships` | |
| 8 | `terms` | |
| 9 | Khởi tạo React (login + layout theo role); trước đó học nhanh React/TS khoảng 10h | |
| 10 | CRUD sự kiện + upload ảnh | |
| 11 | Trang sự kiện công khai + đăng ký | |
| 12 | Check-in + sổ cái điểm | |
| 13 | Finalize sự kiện | |
| 14 | Deploy (đây là mốc MVP) | |
| 15–17 | Shop / Journey, Dashboard, Thông báo | |
| 18 | VietQR | |
| 19–22 | Event Proposal, tuyển CTV, Awards, Ticket | |

Lịch dự kiến (khoảng 6h/ngày; đầu mỗi tuần xem lại tốc độ): tuần 1 (1–7/11) xong Bài 5–7 · tuần 2 Bài 8 + React cơ bản + Bài 9 · tuần 3 Bài 10–12 · tuần 4 Bài 13–14 → MVP khoảng **28/11/2026** · tháng 12 Bài 15–22.

## Trạng thái hiện tại

_Cập nhật: 2026-11-06_

- **Đang làm:** Bài 6, tách `accounts` / `members`. Nhánh `feat/members`.
- **Đã có:** `V3__create_members_table.sql` (bảng `members`: `account_id` UNIQUE FK, `full_name`, `student_code`, `phone`, `avatar_url`, `member_status` mặc định `CTV`, `joined_at`, timestamps), còn dở câu `INSERT ... SELECT` tạo hồ sơ tạm cho các account đã có.
- **Việc tiếp theo:**
  - Hoàn thành migration V3. Lưu ý tên cột `update_at` nên đổi thành `updated_at` **trước khi** migration chạy ở bất kỳ đâu ngoài máy local.
  - Tiếp theo là entity `Member` + enum `MemberStatus` (CTV/ACTIVE/INACTIVE/DISQUALIFIED), repository, tạo member khi HR tạo account, API hồ sơ, test.
- **Còn treo:** HR đặt mật khẩu cho member (gửi link qua email hoặc bắt đổi mật khẩu lần đầu), xem `roadmap.md`.

## Cài đặt máy mới

Chung:
- JDK **25** (Temurin). IntelliJ: Project Structure → SDK → Download JDK → Temurin 25.
- Docker (Docker Desktop trên Mac, chạy được image arm64 của `postgres:17` và Testcontainers).
- Tạo `FEVeID_BackEnd/.env` từ `.env.example` (`DB_PASSWORD`, `JWT_SECRET`). **Không commit file này.** Giá trị `DB_PASSWORD` phải khớp giữa `.env` (compose dùng) và Run Configuration của IntelliJ.
- Postgres: `docker compose up -d` trong `FEVeID_BackEnd/`, map cổng **5433** (`application.properties` trỏ tới `localhost:5433`).
- Run Configuration của app: env `DB_PASSWORD`, `JWT_SECRET`; VM option `-Duser.timezone=Asia/Ho_Chi_Minh`.
- Test: `./mvnw test` (Testcontainers tự bật Postgres; cần Docker đang chạy).

Riêng laptop Windows:
- `JAVA_HOME` hệ thống là JDK 21, `java` trên PATH là 1.8 → `./mvnw` ở terminal báo "release version 25 not supported" nếu không đặt `JAVA_HOME` sang JDK 25 (`C:\Users\Admin\.jdks\temurin-25.0.3`). Chạy test trong IntelliJ thì không bị.
- Terminal mặc định là PowerShell (không có `openssl`); nút Terminal của Sourcetree mở Git Bash.
- Postgres 17 cài trên Windows chiếm cổng 5432, nên mới dùng 5433.
- TZ mặc định của JVM là `Asia/Saigon`, image Postgres trên Debian 13 không nhận → luôn đặt `-Duser.timezone=Asia/Ho_Chi_Minh` (surefire đã đặt sẵn cho test).
- Chưa cài `gh`; kiểm tra GitHub qua `curl https://api.github.com/repos/triet2809/FEVeID/...` (repo public).

Riêng Mac mini:
- Có sẵn `openssl` để tạo `JWT_SECRET`: `openssl rand -base64 48`.
- Nếu `./mvnw` báo không có quyền chạy: `chmod +x mvnw` (file đã có mode 100755 trong git).
