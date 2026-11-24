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
- **Từ Bài 9 (chốt 23/11/2026), chia việc theo độ mới:**
  1. *Mẫu đã quen* (CRUD, DTO, migration, repository, trang danh sách/form giống trang đã làm): Claude viết code trên nhánh; chủ dự án đọc theo "bản đồ luồng" (bước nào chạy ở file:dòng nào) và trả lời 2–3 câu hỏi kiểm tra hiểu trước khi merge.
  2. *Kiến thức mới* (VietQR + webhook, QR check-in HMAC, job hết hạn giữ chỗ, upload ảnh): học trước (giải thích + sơ đồ + thử nhỏ trong playground) → viết test trước → chủ dự án tự viết phần lõi (20–60 dòng quyết định), Claude viết phần khung.
  3. *Tiền / bảo mật*: như loại 2, thêm: chủ dự án giải thích được từng dòng lõi; test các ca gửi trùng, gửi giả, gửi đồng thời.
  - Quy tắc chung: không merge code mà chủ dự án không giải thích được. Khi giải thích luồng frontend, dùng ngôn ngữ đơn giản và so sánh với Spring (Route ≈ @GetMapping, useParams ≈ @PathVariable, RequireAuth ≈ Security filter, TanStack Query ≈ @Cacheable); tránh dồn nhiều thuật ngữ một lúc.
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
| 8.5 | Học nhanh TypeScript + React (A: TS; B1–B4: component, state, useEffect, cập nhật dữ liệu; C1: React Router; C2: TanStack Query; C3: Ant Design) | ✅ 23/11 (playground ngoài repo) |
| 9 | Khởi tạo React (9.1 role trong `/me`, 9.2 JWT trong cookie httpOnly + CSRF, 9.3 khung `FEVeID_FrontEnd` + CI frontend, 9.4 login + layout/menu theo role) | ✅ PR #30, #31, #32, #33 |
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

Ngày 19/11: xong Bài 7 và Bài 8, đúng kế hoạch mới (tuần 3 = Bài 8 + React).

Ngày 23/11: xong 8.5 (TS/React, lâu hơn dự kiến vì chủ dự án hoàn toàn mới với frontend). Bài 9 bắt đầu tuần 4 (24/11); MVP (Bài 14) dự kiến lùi về khoảng 5–8/12.

Ngày 24/11: xong Bài 9 (gồm cả việc đổi sang cookie httpOnly, ngoài kế hoạch). Đúng kế hoạch mới: MVP khoảng 5–8/12.

## Trạng thái hiện tại

_Cập nhật: 2026-11-24 (trên laptop Windows)_

- **Đã xong Bài 1–9**, tất cả nằm trên `master` (#33). Không có nhánh dở dang.
- **Tiếp theo: Bài 10, CRUD sự kiện + upload ảnh** (chủ dự án mở ở phiên mới). Việc đầu tiên:
  - Đọc `requirements.md` mục 2.4, 5.2, 6, 6.1 (sự kiện). Trạng thái: `DRAFT → UPCOMING → ONGOING → COMPLETED` (+ `CANCELLED`), chốt 24/11: UPCOMING/ONGOING/COMPLETED tính từ lịch giai đoạn `event_phases`, không dùng job.
  - Upload ảnh là **kiến thức mới** → loại 2 của mentor mode: giải thích + thử nhỏ trước, viết test trước, chủ dự án tự viết phần lõi. Cần chốt nơi lưu ảnh (Cloudinary hay Cloudflare R2, xem `roadmap.md` "Module upload file dùng chung").
  - CRUD sự kiện là mẫu đã quen → Claude viết, chủ dự án trả lời câu hỏi kiểm tra trước khi merge. Nhớ thêm `@Schema(requiredMode = REQUIRED)` cho DTO mới và chạy `npm run gen:api` trong cùng PR.

### Bài 9 đã làm gì (23–24/11)
- **PR #30 – JWT trong cookie httpOnly** (chủ dự án chọn cookie thay vì localStorage):
  - `POST /api/auth/login` → `204` + `Set-Cookie: access_token` (`HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age` = hạn token), body rỗng; `POST /api/auth/logout` → cookie rỗng `Max-Age=0`. Tạo cookie ở `auth/security/AuthCookies`. `JwtAuthResponse` đã xóa.
  - `JwtAuthenticationFilter`: có header `Authorization` thì **chỉ** dùng header (không lùi về cookie), không có thì đọc cookie. Swagger/Postman/test vẫn dùng Bearer (test lấy token từ cookie trong `tokenOf()`).
  - CSRF: `csrf.spa()` (cookie `XSRF-TOKEN` đọc được bằng JS ↔ header `X-XSRF-TOKEN`), chỉ bắt khi request dùng cookie `access_token` làm danh tính: bỏ qua `/api/auth/login`, request có header `Authorization`, request không có cookie `access_token` (ẩn danh vẫn ra 401 chứ không phải 403). GET không bị kiểm tra → **không bao giờ viết endpoint GET sửa dữ liệu**.
  - `app.jwt.cookie-secure=${JWT_COOKIE_SECURE:true}`; `springdoc.swagger-ui.csrf.enabled=true`. Test `AuthCookieTest` (10 test: thuộc tính cookie, CSRF thiếu/sai/đúng, cookie giả, header sai không lùi về cookie, logout).
- **PR #31** – `GET /api/members/me` trả thêm `role`.
- **PR #32 – khung `FEVeID_FrontEnd`**: Vite 8 + React 19 + TS 6 + antd 6 + TanStack Query 5 + react-router 7 (cấu hình chép từ playground). `src/api/client.ts` là chỗ duy nhất gọi backend (`api.get/post/...`, `HttpError`, `credentials: "same-origin"`); chủ dự án tự viết `readCookie` + gắn header CSRF. Job CI `frontend-build` (`npm ci` → lint → `tsc -b` + `vite build`). `openapi-typescript` 7.13 chỉ khai báo hỗ trợ TS 5 → dùng `overrides` trong `package.json` (đã thử chạy được).
- **PR #33 – login + layout theo role**:
  - Frontend không giữ token nên "đã đăng nhập chưa" = kết quả `GET /api/members/me`, cache key `["me"]` (`auth/useMe.ts`; `useCurrentUser()` dùng bên trong `RequireAuth`).
  - `RequireAuth`: đang tải → Spin; 401 → `/login` kèm `state.from`; lỗi khác → `Result` lỗi. `RequireRole` + `ROLE_GROUPS` (`auth/roles.ts`) dùng chung cho menu (`layouts/AppLayout.tsx`) và route (`App.tsx`, khu nội bộ dưới `/app`).
  - `LoginPage`: login xong `invalidateQueries(["me"])` → `/me` 200 → `if (me.isSuccess) <Navigate>` (cũng chặn người đã đăng nhập vào `/login`). `useLogout`: gọi logout + `queryClient.clear()` + về `/login`. `main.tsx`: query nào 401 (trừ `/me`) → `resetQueries(["me"])` → về `/login`.
  - Backend: `MemberResponse` có `@Schema(requiredMode = REQUIRED)` cho field luôn có giá trị → kiểu TS không có `?`. `src/api/schema.d.ts` đã sinh và commit.
  - Claude đã thử trên trình duyệt (chưa đăng nhập → `/login`, validate form, sai mật khẩu → message backend); luồng đăng nhập thật do chủ dự án tự thử (đã thấy `document.cookie` chỉ có `XSRF-TOKEN`).
- **Quyết định đã chốt (23/11):** form dùng `Form` của antd (không React Hook Form/Zod); API client = `openapi-typescript` (chỉ sinh kiểu) + `fetch` tự viết; khi deploy frontend và `/api` **chung domain** (nginx phục vụ React + proxy `/api`) nên không cần CORS; test frontend (Playwright luồng login/logout) để sau MVP.
- Đã gỡ extension Codacy (VS Code) + `~/.codacy` + `.codacy/` trong playground (Codacy sinh file luật AI `.github/instructions/`).

### Còn treo
- **Thêm `frontend-build` vào required checks** của ruleset "protect master" (hiện chỉ bắt `build` backend → PR vẫn merge được khi CI frontend đỏ). Chủ dự án tự làm trên GitHub.
- **Quy tắc:** đổi DTO backend → chạy backend rồi `npm run gen:api` trong cùng PR (CI không phát hiện `schema.d.ts` cũ). DTO khác ngoài `MemberResponse` vẫn toàn field `?` → thêm `@Schema(requiredMode = REQUIRED)` khi frontend bắt đầu dùng.
- `/me` chưa có ban + chức vụ → menu chưa có mục cho Trưởng/Phó ban; làm khi có trang quản lý ban.
- JWT không thu hồi được khi logout (stateless; chỉ xóa cookie). Chấp nhận cho MVP.
- **Mac mini:** chưa thử frontend. Safari có thể không lưu cookie `Secure` trên `http://localhost` → nếu đăng nhập xong vẫn bị đá về `/login`, đặt `JWT_COOKIE_SECURE=false` trong `.env` (hoặc dùng Chrome).
- Màu chủ đạo `#7E00B2` (`src/theme.ts`) là tạm, đo từ ảnh áo CLB → hỏi người thiết kế lấy mã gốc.
- Buổi tổng kết kỳ có thể kiêm **kết nạp thành viên mới** (CTV → ACTIVE hàng loạt) → nhớ khi làm tuyển CTV.
- Từ trước: HR đặt mật khẩu cho member (`roadmap.md`); HR tự Disqualify chính mình/PRESIDENT; lịch sử đổi trạng thái thành viên; tìm kiếm + phân trang; 2 request tạo kỳ chồng nhau cùng lúc → 500 (`DataIntegrityViolationException`); khoảng trống giữa 2 kỳ (quyết ở Bài 12).

### Ghi chú về cách học (cập nhật 24/11)
- Bài 9 chủ dự án xin code mẫu cho phần frontend ("không rõ code FE") → với frontend, đưa code mẫu có comment so sánh Java ngay, rồi hỏi câu kiểm tra.
- Câu trả lời kiểm tra Bài 9: nhầm "trình duyệt tự gửi cookie" với "JS tự gắn header" (CSRF); tưởng GET thiếu CSRF → 401 (thật ra 200); tưởng quên `queryClient.clear()` thì "còn token, báo 401" (thật ra lộ dữ liệu người trước, không có lỗi gì). Đúng: thêm trang mới = route + trang + mục menu. Hay muốn bỏ qua câu hỏi kiểm tra ("tôi hiểu rồi, qua tiếp") → vẫn hỏi ngắn, ghi lại câu chưa trả lời.
- Vẫn chạy `npm` ở gốc repo (lần này `npm run dev` "không chạy được" vì đứng ở `D:\FEVeID`) → luôn nhắc `cd FEVeID_FrontEnd`, dấu nhắc phải là `PS D:\FEVeID\FEVeID_FrontEnd>`.
- Đã bật format khi lưu (commit `style: format LoginPage`).

### Bài học kỹ thuật cũ vẫn cần nhớ (17–19/11)
- `@PreAuthorize` gọi bean: tên `#...` phải trùng tên tham số Java; trả 403 (không phải 404) cho ban không tồn tại.
- Self-invocation không đi qua proxy → `@Transactional`/`@Cacheable` bị bỏ qua.
- `@NotBlank` chỉ cho `String`; `LocalDate` dùng `@NotNull`. Validation chéo: `@AssertTrue isXxx()` trả `true` khi field null. Field bắt buộc trong request dùng `Boolean` (không `boolean`).
- Git: tạo nhánh từ `origin/master` (không từ nhánh cũ); `AM` trong `git status` → `git add -A` trước khi commit; nhận review nhiều mục thì làm checklist.

## Cài đặt máy mới

Chung:
- JDK **25** (Temurin). IntelliJ: Project Structure → SDK → Download JDK → Temurin 25.
- Docker (Docker Desktop trên Mac, chạy được image arm64 của `postgres:17` và Testcontainers).
- Tạo `FEVeID_BackEnd/.env` từ `.env.example` (`DB_PASSWORD`, `JWT_SECRET`). **Không commit file này.** Giá trị `DB_PASSWORD` phải khớp giữa `.env` (compose dùng) và Run Configuration của IntelliJ.
- Postgres: `docker compose up -d` trong `FEVeID_BackEnd/`, map cổng **5433** (`application.properties` trỏ tới `localhost:5433`).
- Run Configuration của app: env `DB_PASSWORD`, `JWT_SECRET`, và cho profile dev: `SPRING_PROFILES_ACTIVE=dev`, `DEV_HR_EMAIL`, `DEV_HR_PASSWORD`; VM option `-Duser.timezone=Asia/Ho_Chi_Minh`. Spring Boot **không tự đọc** `.env` (chỉ `docker compose` đọc); muốn dùng chung một chỗ thì bật plugin EnvFile trong Run Configuration và trỏ tới `FEVeID_BackEnd/.env`.
- Test: `./mvnw test` (Testcontainers tự bật Postgres; cần Docker đang chạy).
- Frontend: Node **24** + npm. Trong `FEVeID_FrontEnd/`: `npm install` (lần đầu), `npm run dev` → http://localhost:5173 (cần backend ở 8080). Không chạy npm ở gốc repo.

Riêng laptop Windows:
- `JAVA_HOME` hệ thống là JDK 21, `java` trên PATH là 1.8 → `./mvnw` ở terminal báo "release version 25 not supported" nếu không đặt `JAVA_HOME` sang JDK 25 (`C:\Users\Admin\.jdks\temurin-25.0.3`). Chạy test trong IntelliJ thì không bị.
- Terminal mặc định là PowerShell (không có `openssl`); nút Terminal của Sourcetree mở Git Bash.
- Postgres 17 cài trên Windows chiếm cổng 5432, nên mới dùng 5433.
- TZ mặc định của JVM là `Asia/Saigon`, image Postgres trên Debian 13 không nhận → luôn đặt `-Duser.timezone=Asia/Ho_Chi_Minh` (surefire đã đặt sẵn cho test).
- Chưa cài `gh`; kiểm tra GitHub qua `curl https://api.github.com/repos/triet2809/FEVeID/...` (repo public).
- Chạy Maven ở PowerShell: đứng trong `FEVeID_BackEnd/` rồi `$env:JAVA_HOME="C:\Users\Admin\.jdks\temurin-25.0.3"; .\mvnw.cmd test` (đứng ở gốc repo sẽ báo `./mvnw not recognized`).
- Docker Desktop hay tự tắt (đã gặp 3 lần ngày 06–07/11) → test lỗi `Previous attempts to find a Docker environment failed`. Chạy `docker ps` trước khi test; nên bật Settings → General → *Start Docker Desktop when you sign in*. Máy còn có các container `nexus-*` của dự án khác chiếm cổng 5432.
- `node`/`npm` trên PATH lấy từ `D:\OpenClawAI\node.exe` (v24.16).
- IntelliJ IDEA Ultimate 2026.2: có JPA Buddy (Ctrl+Shift+A → "Entities from DB"), plugin EnvFile đã cài.

Riêng Mac mini:
- Có sẵn `openssl` để tạo `JWT_SECRET`: `openssl rand -base64 48`.
- Nếu `./mvnw` báo không có quyền chạy: `chmod +x mvnw` (file đã có mode 100755 trong git).
- Đã setup: Temurin 25 (arm64, là `java` mặc định), Docker Desktop, `.env`, container `feveid-postgres` (volume `feveid_backend_feveid-pgdata`), Sourcetree, `gh` (đã đăng nhập, protocol https). **Chưa kiểm tra Node** (frontend cần v24: `node -v`).
- Repo ở `/Users/mac/FEVeID/FEVeID`. `./mvnw` và `docker compose` phải chạy trong `FEVeID_BackEnd/`; lệnh git chạy ở đâu trong repo cũng được.
- Máy **không có SSH key** → remote `origin` phải dùng HTTPS (`https://github.com/triet2809/FEVeID.git`). Nếu Sourcetree/git báo `Permission denied (publickey)` thì remote đang là `ssh://...`: đổi lại sang HTTPS.
