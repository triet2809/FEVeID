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
| 6 | Tách `accounts` / `members` (6.1 entity, 6.2 tạo Member khi HR tạo account, 6.3 API hồ sơ + HR đổi trạng thái) | ⏳ 6.1 ✅ PR #12, 6.2 ✅ PR #13, 6.3 chưa làm |
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

Xem lại tốc độ ngày 09/11: tuần 1 mới xong Bài 5 và khoảng 70% Bài 6 (chậm khoảng 1,5 bài). Kế hoạch mới: tuần 2 xong Bài 6 + Bài 7, tuần 3 Bài 8 + React; MVP có thể lùi về khoảng 1–2/12.

## Trạng thái hiện tại

_Cập nhật: 2026-11-09 (trên laptop Windows)_

- **Đã xong Bài 6.1 và 6.2** (đã nằm trên `master`):
  - PR #12: entity `Member` (`@OneToOne(fetch = LAZY)` với `Account`, `MemberStatus` dùng `@Enumerated(STRING)`, `@Builder.Default` cho `memberStatus = CTV` và `joinedAt = LocalDate.now()`, `@CreationTimestamp`/`@UpdateTimestamp`), `MemberRepository` (`findByAccountId`, `existsByStudentCode`), `V4__rename_members_update_at.sql`, `MemberRepositoryTest` (3 test).
  - PR #13: `POST /api/accounts/register` nhận thêm `fullName` (bắt buộc) và `studentCode` (không bắt buộc), tạo `Member` trong cùng `@Transactional`; trùng mã sinh viên → 409 "Mã số sinh viên đã được sử dụng". `AccountRegistrationTest` có 4 test, trong đó có test chứng minh rollback (gọi thẳng service với `fullName` 101 ký tự).
  - Tổng cộng **26 test** xanh.
- **Đang làm:** PR nhỏ `feat/swagger-jwt-dev-seed`, làm trên laptop Windows, **chưa push**:
  - `common/config/OpenApiConfig.java`: khai báo security scheme `bearerAuth` để Swagger có nút Authorize.
  - `common/config/DevDataSeeder.java`: `@Profile("dev")`, `CommandLineRunner` tạo account HR + `Member` nếu chưa có email đó; email/mật khẩu lấy từ env `DEV_HR_EMAIL`, `DEV_HR_PASSWORD` (không ghi cứng trong code vì repo public).
  - `.env.example` đã thêm `DEV_HR_EMAIL=`, `DEV_HR_PASSWORD=` (để trống).
  - Kiểm tra bằng Swagger: login HR → Authorize → `POST /api/accounts/register` → 201; trùng `studentCode` → 409. CI xanh thì tự merge (PR cấu hình).
- **Việc tiếp theo: Bài 6.3, API hồ sơ** (Claude đưa khung + TODO, test-first):
  - `GET /api/members/me`, `PATCH /api/members/me` (member chỉ sửa được `phone`, `avatarUrl`; FR 2.1).
  - `GET /api/members`, `GET /api/members/{id}` (HR/PRESIDENT).
  - `PATCH /api/members/{id}/status` (HR: ACTIVE/INACTIVE/DISQUALIFIED; FR 4.1).
  - Xong 6.3 thì tick mục "Tách `accounts` và `members`" trong `roadmap.md`, chuyển sang Bài 7.
  - Để sang bài sau: gán ban/chức vụ (Bài 7), lịch sử đổi trạng thái (cần bảng riêng), đổi mật khẩu, tìm kiếm nâng cao.
- **Bài học trong phiên này:**
  - Đọc log dài: tìm dòng `Caused by:` sâu nhất của lỗi **đầu tiên**; các lỗi sau thường chỉ là `ApplicationContext failure threshold exceeded`. Với Lombok thì đọc dòng WARNING ngay phía trên ERROR.
  - Class test phải có tên kết thúc bằng `Test`/`Tests` thì Surefire (và CI) mới chạy. Đã gặp: `AccountRegistration` bị bỏ qua mà CI vẫn xanh.
  - Cả class test cùng đỏ một lỗi → xem `@BeforeEach` trước (đã gặp: gõ sai email khi lấy token).
  - Không đặt `@Transactional` lên class test có test rollback, nếu không test đó luôn xanh.
  - Vẫn còn auto-import nhầm package (`org.testcontainers.utility.TestcontainersConfiguration`) và merge PR `feat/` trước khi Claude review.
- **Còn treo:** HR đặt mật khẩu cho member (gửi link qua email hoặc bắt đổi mật khẩu lần đầu), xem `roadmap.md`.

## Cài đặt máy mới

Chung:
- JDK **25** (Temurin). IntelliJ: Project Structure → SDK → Download JDK → Temurin 25.
- Docker (Docker Desktop trên Mac, chạy được image arm64 của `postgres:17` và Testcontainers).
- Tạo `FEVeID_BackEnd/.env` từ `.env.example` (`DB_PASSWORD`, `JWT_SECRET`). **Không commit file này.** Giá trị `DB_PASSWORD` phải khớp giữa `.env` (compose dùng) và Run Configuration của IntelliJ.
- Postgres: `docker compose up -d` trong `FEVeID_BackEnd/`, map cổng **5433** (`application.properties` trỏ tới `localhost:5433`).
- Run Configuration của app: env `DB_PASSWORD`, `JWT_SECRET`, và cho profile dev: `SPRING_PROFILES_ACTIVE=dev`, `DEV_HR_EMAIL`, `DEV_HR_PASSWORD`; VM option `-Duser.timezone=Asia/Ho_Chi_Minh`. Spring Boot **không tự đọc** `.env` (chỉ `docker compose` đọc); muốn dùng chung một chỗ thì bật plugin EnvFile trong Run Configuration và trỏ tới `FEVeID_BackEnd/.env`.
- Test: `./mvnw test` (Testcontainers tự bật Postgres; cần Docker đang chạy).

Riêng laptop Windows:
- `JAVA_HOME` hệ thống là JDK 21, `java` trên PATH là 1.8 → `./mvnw` ở terminal báo "release version 25 not supported" nếu không đặt `JAVA_HOME` sang JDK 25 (`C:\Users\Admin\.jdks\temurin-25.0.3`). Chạy test trong IntelliJ thì không bị.
- Terminal mặc định là PowerShell (không có `openssl`); nút Terminal của Sourcetree mở Git Bash.
- Postgres 17 cài trên Windows chiếm cổng 5432, nên mới dùng 5433.
- TZ mặc định của JVM là `Asia/Saigon`, image Postgres trên Debian 13 không nhận → luôn đặt `-Duser.timezone=Asia/Ho_Chi_Minh` (surefire đã đặt sẵn cho test).
- Chưa cài `gh`; kiểm tra GitHub qua `curl https://api.github.com/repos/triet2809/FEVeID/...` (repo public).
- Chạy Maven ở PowerShell: đứng trong `FEVeID_BackEnd/` rồi `$env:JAVA_HOME="C:\Users\Admin\.jdks\temurin-25.0.3"; .\mvnw.cmd test` (đứng ở gốc repo sẽ báo `./mvnw not recognized`).
- Docker Desktop hay tự tắt (đã gặp 3 lần ngày 06–07/11) → test lỗi `Previous attempts to find a Docker environment failed`. Chạy `docker ps` trước khi test; nên bật Settings → General → *Start Docker Desktop when you sign in*. Máy còn có các container `nexus-*` của dự án khác chiếm cổng 5432.
- IntelliJ IDEA Ultimate 2026.2: có JPA Buddy (Ctrl+Shift+A → "Entities from DB"), plugin EnvFile đã cài.

Riêng Mac mini:
- Có sẵn `openssl` để tạo `JWT_SECRET`: `openssl rand -base64 48`.
- Nếu `./mvnw` báo không có quyền chạy: `chmod +x mvnw` (file đã có mode 100755 trong git).
- Đã setup: Temurin 25 (arm64, là `java` mặc định), Docker Desktop, `.env`, container `feveid-postgres` (volume `feveid_backend_feveid-pgdata`), Sourcetree, `gh` (đã đăng nhập, protocol https).
- Repo ở `/Users/mac/FEVeID/FEVeID`. `./mvnw` và `docker compose` phải chạy trong `FEVeID_BackEnd/`; lệnh git chạy ở đâu trong repo cũng được.
- Máy **không có SSH key** → remote `origin` phải dùng HTTPS (`https://github.com/triet2809/FEVeID.git`). Nếu Sourcetree/git báo `Permission denied (publickey)` thì remote đang là `ssh://...`: đổi lại sang HTTPS.
