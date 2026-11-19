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
| 6 | Tách `accounts` / `members` (6.1 entity, 6.2 tạo Member khi HR tạo account, 6.3 API hồ sơ + HR đổi trạng thái) | ✅ PR #12, #13, #15, #18, #20 |
| 7 | `departments` + `department_memberships` (7.1 CRUD ban, 7.2 gán/chuyển ban + chức vụ, 7.3 phân quyền Trưởng ban) | ✅ PR #22, #23, #24 |
| 8 | `terms` (8.1 CRUD + mở/đóng gian hàng, 8.2 kỳ hiện tại + `Clock` + lọc gian hàng đang mở) | ✅ PR #25, #26 |
| 8.5 | Học nhanh TypeScript + React (khoảng 10h, làm trong thư mục playground ngoài repo) | ⏳ đang làm |
| 9 | Khởi tạo React (login + layout theo role); trước đó học nhanh React/TS khoảng 10h | |
| 10 | CRUD sự kiện + upload ảnh | |
| 11 | Trang sự kiện công khai + đăng ký | |
| 12 | Check-in + sổ cái điểm | |
| 13 | Finalize sự kiện | |
| 14 | Deploy (đây là mốc MVP) | |
| 14.5 | Landing page khu công khai + hiệu ứng (Motion), theo thiết kế chủ dự án tự làm | |
| 15–17 | Shop / Journey, Dashboard, Thông báo | |
| 18 | VietQR | |
| 19–22 | Event Proposal, tuyển CTV, Awards, Ticket | |

Lịch dự kiến (khoảng 6h/ngày; đầu mỗi tuần xem lại tốc độ): tuần 1 (1–7/11) xong Bài 5–7 · tuần 2 Bài 8 + React cơ bản + Bài 9 · tuần 3 Bài 10–12 · tuần 4 Bài 13–14 → MVP khoảng **28/11/2026** · tháng 12 Bài 15–22.

Xem lại tốc độ ngày 09/11: tuần 1 mới xong Bài 5 và khoảng 70% Bài 6 (chậm khoảng 1,5 bài). Kế hoạch mới: tuần 2 xong Bài 6 + Bài 7, tuần 3 Bài 8 + React; MVP có thể lùi về khoảng 1–2/12.

Ngày 11/11: xong Bài 6, bắt đầu Bài 7, đúng kế hoạch mới.

Ngày 19/11: xong Bài 7 và Bài 8, đúng kế hoạch mới (tuần 3 = Bài 8 + React). Tiếp theo: học nhanh TS/React (8.5) rồi Bài 9.

## Trạng thái hiện tại

_Cập nhật: 2026-11-19 tối (trên laptop Windows)_

- **Đã xong Bài 7** (đã nằm trên `master`):
  - PR #22: CRUD ban. `GET /api/departments`, `GET /api/departments/{id}` công khai (`permitAll` chỉ cho method GET); `POST`/`PUT` chỉ PRESIDENT; tên ban unique không phân biệt hoa thường (`LOWER(name)`) → trùng thì 409. Chưa làm xóa ban.
  - PR #23: `department_memberships` (V6). `end_date IS NULL` = đang giữ; chuyển ban / đổi chức vụ = đóng dòng cũ + mở dòng mới (có lịch sử cho Personal Journey). Partial unique index: mỗi member một ban, mỗi ban một `LEADER`. `PUT /api/members/{id}/department` (HR, idempotent), `DELETE` (rút khỏi ban), `GET /api/departments/{id}/members` (Trưởng → Phó → Thành viên, rồi theo tên). Member bị Disqualify → đóng membership.
  - PR #24: bean `@departmentAccess` (`isManagerOf`, `isManagerOfMember`). Trưởng ban **và Phó ban** xem được danh sách và hồ sơ thành viên ban mình; ban khác, ban không tồn tại, cựu Trưởng ban → 403. Gán ban vẫn chỉ HR. Luật "chức vụ nào là quản lý" nằm ở `DepartmentPosition.isManager()`.
- **Bài 8.1 xong** (PR #25): bảng `terms` (V7) gồm `name`, `start_date`, `end_date`, `shop_open`. Ràng buộc `EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&)` chặn 2 kỳ chồng nhau ở DB; service kiểm tra trước để trả 409. `@AssertTrue isDateRangeValid()` trong `TermRequest` kiểm tra chéo ngày. API: `GET /api/terms`, `GET /api/terms/{id}` (đã đăng nhập), `POST`/`PUT` (HR, PRESIDENT), `PATCH /api/terms/{id}/shop` body `{"open": true}` (idempotent).
- **Nghiệp vụ đã chốt (18/11)**, đã ghi vào `requirements.md` mục 11.2 và `CLAUDE.md`: kỳ = kỳ học Fall/Spring/Summer, điểm tính riêng; mỗi kỳ một gian hàng, HR mở/đóng thủ công; một buổi tổng kết có thể mở cùng lúc gian hàng của nhiều kỳ. Có buổi tổng kết kiêm **kết nạp thành viên mới** (CTV → ACTIVE hàng loạt) → nhớ khi làm tuyển CTV.
- **Bài 8.2 xong** (PR #26): bean `Clock` trong `common/config/TimeConfig` (`CLUB_ZONE = Asia/Ho_Chi_Minh`); service lấy ngày bằng `LocalDate.now(clock)`, test thay bằng `@MockitoBean Clock` (`CurrentTermTest`, có case 00:30 ngày 01/01 giờ VN để bắt lỗi dùng nhầm UTC). `@Query` JPQL đầu tiên: `TermRepository.findContaining(date)`. `GET /api/terms/current` (không có kỳ → 404), `GET /api/terms?shopOpen=true|false`.
- **Thiết kế frontend đã chốt (20/11)**, ghi ở `CLAUDE.md` mục Frontend: nhiều hiệu ứng chỉ ở khu công khai (tham khảo game.kyucdisan.com: Next.js + video nền + GSAP, font Alegreya + Be Vietnam Pro), khu nội bộ dùng Ant Design gọn; làm hiệu ứng sau MVP (Bài 14.5); chủ dự án tự thiết kế, CLB đã có tài nguyên.
- **Đang làm: Bài 8.5, học nhanh TypeScript + React** trước Bài 9. Code học nằm ở thư mục playground **ngoài repo** (laptop: `D:
eact-playground`), không commit vào FEVeID. Node 24 + npm 11 đã có trên laptop. Tiến độ: `src/learn/01-types.ts` xong (type, union, `find`/`filter`/`toSorted`/`map`, narrowing, `?.`/`??`; đã hiểu kiểu chỉ kiểm tra lúc biên dịch, lúc chạy bị xóa → cần Zod cho dữ liệu API). Chủ dự án **hoàn toàn mới với JS/frontend** → code mẫu cần comment giải thích kỹ, so sánh với Java. Tiếp theo: `02-fetch.ts` (login + `fetchTerms` + `Result<T>`, cần backend profile dev). Lộ trình: (A) TypeScript cho người biết Java, (B) React core với Vite, gọi API backend qua Vite proxy, (C) React Router + TanStack Query + Ant Design.
- **Bài học trong các phiên 17–19/11:**
  - `@PreAuthorize` gọi bean (`@departmentAccess.isManagerOf(#departmentId, authentication.name)`): tên `#...` phải trùng tên tham số Java. Trả 403 (không phải 404) cho ban không tồn tại để người ngoài không dò được.
  - Self-invocation: gọi method trong cùng class thì không đi qua proxy → `@Transactional`/`@Cacheable` của method được gọi bị bỏ qua.
  - `@NotBlank` chỉ dùng cho `String`; gắn vào `LocalDate` → `UnexpectedTypeException` (500). Jackson đổi `""`/`"   "` thành `null` cho ngày, nên `@NotNull` là đủ; sai định dạng → `HttpMessageNotReadableException` → 400.
  - Validation chéo nhiều field: `@AssertTrue` trên method `isXxx()`, nhớ trả `true` khi field null để `@NotNull` tự báo lỗi.
  - `Boolean` (không phải `boolean`) cho field bắt buộc trong request: thiếu field → `null` → 400, thay vì ngầm là `false`.
  - Vẫn còn: **tạo nhánh từ nhánh cũ** (PR #25, phải `git reset --soft origin/master`) → luôn chuột phải `origin/master` → Branch; **`AM` trong `git status`** = đã stage bản cũ rồi sửa tiếp → `git add -A` trước khi commit; chưa bật format khi lưu (phải nhắc 3 lần mới format; đã dọn xong trong PR #26) → bật *Actions on Save*; nhận review nhiều mục thì làm checklist kẻo sót (PR #26 sót `getTerms()` ở interface → lỗi biên dịch, IntelliJ báo "related problems" ở `TimeConfig`; xem lỗi thật ở Problems → Project Errors); hay bỏ qua bước dự đoán / câu hỏi kiểm tra. Đã trả lời 3 câu của 8.1: câu 2 sai (test bị chặn ở `@Valid` thì không kiểm tra được repository; Spring Data gán tham số theo vị trí), câu 3 chưa phân biệt checked/unchecked exception và chưa biết NPE trong validator → `ValidationException` → 500. 2 câu của 8.2 (UTC, bỏ endpoint `/current`) chưa trả lời.
- **Còn treo:**
  - HR đặt mật khẩu cho member (gửi link qua email hoặc bắt đổi mật khẩu lần đầu), xem `roadmap.md`.
  - HR có thể tự Disqualify chính mình hoặc PRESIDENT → cân nhắc chặn ở một PR `fix/`.
  - Lịch sử đổi trạng thái thành viên (FR 4.1) cần bảng riêng; tìm kiếm theo tên (`Specification`); phân trang danh sách.
  - Hai request tạo kỳ chồng nhau cùng lúc → DB chặn nhưng trả 500 (`DataIntegrityViolationException` chưa có handler) → xử lý khi làm shop.
  - Khoảng trống giữa 2 kỳ: sự kiện rơi vào khoảng trống không thuộc kỳ nào → quyết định ở Bài 12 (sổ cái điểm).

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
