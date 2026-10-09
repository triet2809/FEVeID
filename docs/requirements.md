# Functional Requirements – Club Management System (FEVeID)

> Nguồn gốc: chuyển từ `docs/Context.docx`. Từ nay file này là bản chính thức, mọi thay đổi FR sửa trực tiếp ở đây.
> Mục 1–10 giữ nguyên nội dung gốc. Mục 11–12 được **bổ sung ngày 09/10/2026** theo các quyết định đã chốt (mục 11 trong bản gốc còn trống).

---

## 1. Guest / Người chưa đăng nhập

- Xem thông tin giới thiệu CLB.
- Xem lịch sử hình thành và phát triển của CLB.
- Xem cơ cấu tổ chức và các ban trong CLB.
- Xem các sự kiện công khai của CLB.
- Xem các thành tích nổi bật của CLB.
- Xem các đợt tuyển CTV đang mở.
- Đăng ký ứng tuyển CTV bằng cách gửi thông tin cá nhân và ban mong muốn tham gia.
- Xem thông tin liên hệ và các kênh truyền thông của CLB.

## 2. Member / Thành viên CLB

### 2.1. Authentication & Profile
- Đăng nhập bằng tài khoản do HR cấp.
- Đăng xuất khỏi hệ thống.
- Xem thông tin cá nhân.
- Cập nhật một số thông tin cá nhân được cho phép như avatar, số điện thoại, thông tin liên hệ.
- Đổi mật khẩu tài khoản.

### 2.2. Club Information
- Xem thông tin CLB.
- Xem lịch sử CLB.
- Xem cơ cấu tổ chức CLB.
- Xem thông tin các ban.
- Xem danh sách thành viên trong ban của mình.

### 2.3. Personal Journey
- Xem ngày gia nhập CLB.
- Xem lịch sử các ban từng tham gia.
- Xem lịch sử chức vụ trong CLB.
- Xem danh sách sự kiện đã tham gia.
- Xem danh hiệu và thành tích cá nhân.
- Xem timeline hành trình hoạt động của bản thân trong CLB.

### 2.4. Event
- Xem danh sách sự kiện.
- Xem chi tiết sự kiện.
- Tìm kiếm và lọc sự kiện.
- Đánh dấu sự kiện quan tâm vào wishlist.
- Hủy sự kiện khỏi wishlist.
- Đăng ký mong muốn tham gia tổ chức một sự kiện.
- Xem danh sách các sự kiện đã đăng ký hoặc quan tâm.

### 2.5. Event Proposal
- Tạo đề xuất tổ chức một sự kiện mới.
- Nhập thông tin kế hoạch của sự kiện.
- Đính kèm tài liệu hoặc file kế hoạch.
- Gửi đề xuất cho Ban Chủ nhiệm xét duyệt.
- Theo dõi trạng thái đề xuất.
- Chỉnh sửa đề xuất khi Ban Chủ nhiệm yêu cầu bổ sung.
- Nhận kết quả Approved / Rejected từ Ban Chủ nhiệm.

### 2.6. Ticket
- Tạo Support Ticket khi cần hỗ trợ.
- Tạo Report Ticket để báo cáo vấn đề, tranh chấp hoặc vi phạm.
- Đính kèm minh chứng trong ticket.
- Theo dõi trạng thái ticket.
- Xem phản hồi từ người xử lý.
- Trao đổi thêm thông tin trong ticket.
- Đóng ticket sau khi vấn đề được giải quyết.

### 2.7. Notification
- Xem thông báo chung của CLB.
- Xem thông báo dành riêng cho ban của mình.
- Xem thông báo cá nhân.
- Đánh dấu thông báo đã đọc/chưa đọc.
- Nhận email đối với các thông báo quan trọng.

## 3. Ban Điều Hành / Department Leader

Ban Điều Hành có toàn bộ quyền cơ bản của Member và các chức năng bổ sung sau.

### 3.1. Department Management
- Xem toàn bộ thành viên thuộc ban mình quản lý.
- Xem thông tin và lịch sử hoạt động của từng thành viên.
- Tìm kiếm và lọc thành viên trong ban.
- Xem trạng thái hoạt động của thành viên.
- Đánh giá mức độ hoạt động của thành viên.
- Đề xuất Active / Inactive / Disqualify thành viên cho HR xử lý.
- Đề xuất thay đổi chức vụ hoặc vai trò của thành viên trong ban.

### 3.2. Department Notification
- Tạo thông báo dành riêng cho ban.
- Chỉnh sửa hoặc xóa thông báo do mình tạo.
- Gửi email hàng loạt tới thành viên trong ban.

### 3.3. Department Event
- Xem danh sách thành viên trong ban đăng ký tham gia từng sự kiện.
- Đề xuất thành viên tham gia sự kiện.
- Theo dõi hoạt động của thành viên trong sự kiện.

## 4. HR / Human Resources

HR có các quyền quản lý nhân sự của toàn bộ CLB.

### 4.1. Member Management
- Xem toàn bộ thành viên CLB.
- Tìm kiếm và lọc thành viên.
- Xem hồ sơ chi tiết của thành viên.
- Tạo tài khoản cho thành viên mới.
- Gán thành viên vào ban.
- Chuyển thành viên giữa các ban.
- Gán chức vụ cho thành viên.
- Thay đổi role của thành viên.
- Active tài khoản thành viên.
- Chuyển thành viên sang trạng thái Inactive.
- Disqualify thành viên.
- Khôi phục trạng thái thành viên khi được phép.
- Xem lịch sử thay đổi trạng thái của thành viên.

### 4.2. Recruitment Management
- Tạo đợt tuyển CTV.
- Thiết lập thời gian mở và đóng đơn.
- Thiết lập các ban đang tuyển.
- Đóng/mở đợt tuyển.
- Xem danh sách ứng viên.
- Xem chi tiết hồ sơ ứng viên.
- Lọc ứng viên theo ban hoặc trạng thái.
- Cập nhật trạng thái tuyển dụng của ứng viên. Ví dụ: `APPLIED → SCREENING → INTERVIEW → PASSED / REJECTED`
- Duyệt ứng viên thành CTV.
- Duyệt CTV trở thành thành viên chính thức.
- Tạo tài khoản cho ứng viên sau khi được duyệt.
- Gán thành viên mới vào ban phù hợp.

### 4.3. Ticket Management
- Xem các Report Ticket liên quan đến nhân sự.
- Nhận xử lý ticket.
- Yêu cầu người tạo cung cấp thêm thông tin.
- Cập nhật trạng thái ticket.
- Giải quyết tranh chấp.
- Đóng ticket sau khi xử lý.
- Xem lịch sử xử lý ticket.

### 4.4. Event Interest
- Xem danh sách thành viên wishlist từng sự kiện.
- Xem danh sách thành viên mong muốn tham gia tổ chức sự kiện.
- Lọc thành viên theo ban.
- Hỗ trợ BCN lựa chọn nhân sự cho sự kiện.

## 5. Ban Chủ Nhiệm / President

Ban Chủ Nhiệm có quyền quản trị cao nhất đối với hoạt động CLB.

### 5.1. Club Management
- Xem toàn bộ thành viên CLB.
- Xem toàn bộ các ban.
- Xem thống kê hoạt động của từng ban.
- Xem thống kê thành viên.
- Quản lý thông tin chung của CLB.
- Quản lý lịch sử CLB.
- Quản lý cơ cấu tổ chức CLB.

### 5.2. Event Management
- Tạo sự kiện mới.
- Chỉnh sửa thông tin sự kiện.
- Xóa hoặc hủy sự kiện.
- Thiết lập thời gian sự kiện.
- Thiết lập thời gian đăng ký.
- Thiết lập ban phụ trách.
- Thiết lập số lượng thành viên.
- Thay đổi trạng thái sự kiện. Trạng thái có thể gồm: `DRAFT → OPEN_REGISTRATION → UPCOMING → ONGOING → COMPLETED`, ngoài ra `CANCELLED`.
- Xem danh sách thành viên quan tâm sự kiện.
- Xem danh sách thành viên đăng ký tham gia.
- Phân công thành viên hoặc ban cho sự kiện.

### 5.3. Event Proposal Approval
- Xem các Event Proposal mới.
- Xem nội dung kế hoạch.
- Xem file đính kèm.
- Approve đề xuất.
- Reject đề xuất.
- Yêu cầu người gửi chỉnh sửa.
- Gửi feedback cho người đề xuất.
- Chuyển Event Proposal được duyệt thành Event chính thức.
- Workflow: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED`, hoặc `REVISION_REQUIRED`.

### 5.4. Ticket Management
- Xem các ticket gửi trực tiếp đến BCN.
- Phân công người xử lý ticket.
- Xem tiến độ xử lý.
- Can thiệp vào ticket khi cần.
- Đưa ra quyết định cuối cùng đối với các tranh chấp quan trọng.

### 5.5. Announcement
- Đăng thông báo toàn CLB.
- Đăng thông báo cho một hoặc nhiều ban.
- Gửi email toàn CLB.
- Gửi email cho nhóm thành viên cụ thể.
- Chỉnh sửa hoặc xóa thông báo.

## 6. Event Management System

Mỗi sự kiện cần quản lý:
- Tên sự kiện.
- Mô tả.
- Banner/Image.
- Địa điểm.
- Thời gian bắt đầu.
- Thời gian kết thúc.
- Deadline đăng ký.
- Người phụ trách.
- Ban phụ trách.
- Số lượng thành viên dự kiến.
- Trạng thái sự kiện.
- Danh sách thành viên tham gia.
- Danh sách member wishlist.

Sau khi sự kiện hoàn thành:
- Lưu sự kiện vào lịch sử CLB.
- Cập nhật sự kiện vào Journey của thành viên đã tham gia.
- Cho phép BCN ghi nhận thành tích hoặc danh hiệu.

## 7. Award & Recognition Management

**Member**
- Xem các danh hiệu đã nhận.
- Xem lý do nhận danh hiệu.
- Xem thời gian nhận danh hiệu.
- Xem sự kiện liên quan đến danh hiệu nếu có.

**HR / BCN**
- Tạo danh hiệu.
- Trao danh hiệu cho thành viên.
- Nhập lý do trao danh hiệu.
- Gắn danh hiệu với sự kiện.
- Thu hồi danh hiệu nếu cần.

Ví dụ: Member of the Month, Outstanding Member, Best Contributor, Event MVP, Best Leader.

## 8. Notification System

Hệ thống hỗ trợ các loại thông báo:
- Thông báo toàn CLB.
- Thông báo theo ban.
- Thông báo cá nhân.
- Thông báo liên quan đến event.
- Thông báo liên quan đến ticket.
- Thông báo tuyển dụng.

Người dùng có thể:
- Xem danh sách thông báo.
- Xem thông báo chưa đọc.
- Đánh dấu đã đọc.
- Nhận email đối với thông báo quan trọng.

## 9. Ticket Management System

Ticket có hai loại chính:

**SUPPORT** – dùng cho: lỗi tài khoản, lỗi hệ thống, yêu cầu hỗ trợ, yêu cầu thay đổi thông tin.

**REPORT** – dùng cho: báo cáo vi phạm, tranh chấp, khiếu nại, báo cáo hành vi của thành viên.

Trạng thái: `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED`, ngoài ra có thể có `REJECTED`.

Mỗi ticket lưu: người tạo, loại ticket, tiêu đề, nội dung, minh chứng, người xử lý, priority, trạng thái, ngày tạo, ngày cập nhật, lịch sử trao đổi.

## 10. Dashboard & Statistics

**Member** xem:
- Số sự kiện đã tham gia.
- Số danh hiệu.
- Các hoạt động gần đây.

**Ban Điều Hành** xem:
- Tổng số thành viên ban.
- Active member.
- Inactive member.
- Thành viên tham gia event.
- Hoạt động gần đây của ban.

**HR** xem:
- Tổng thành viên CLB.
- Thành viên theo từng ban.
- Active / Inactive / Disqualified.
- Số ứng viên mỗi đợt tuyển.
- Tỷ lệ ứng viên Passed / Rejected.
- Ticket nhân sự chưa giải quyết.

**BCN** xem tổng quan:
- Tổng số thành viên.
- Tổng số ban.
- Tổng số sự kiện.
- Event sắp diễn ra.
- Event đang diễn ra.
- Ticket đang mở.
- Event Proposal chờ duyệt.
- Recruitment đang diễn ra.
- Thành viên hoạt động nhiều nhất.

---

## 11. Điểm hoạt động & Shop đổi quà *(bổ sung 09/10/2026)*

Sau mỗi kỳ hoạt động, CLB tổ chức buổi tổng kết để vinh danh và cảm ơn thành viên. Thành viên tích điểm qua các sự kiện trong kỳ và dùng điểm để đăng ký nhận quà tại buổi tổng kết.

### 11.1. Quy tắc tích điểm

| Hoạt động | Điểm | Ghi nhận bởi |
|---|---|---|
| Tổ chức xong một sự kiện | +4 | HR đánh dấu đã tổ chức |
| Hỗ trợ xong một sự kiện | +2 | HR đánh dấu đã hỗ trợ |
| Tham gia một sự kiện | +1 | Tự động qua check-in (xem mục 12) |

- Mỗi thành viên chỉ có **một vai trò** trong một sự kiện và nhận điểm theo vai trò đó (không cộng dồn tổ chức + tham gia).
- Đăng ký nhưng không đến (không check-in) → không được cộng điểm, không bị phạt.
- Điểm được phát khi HR **chốt sự kiện** (Finalize): HR xác nhận danh sách tổ chức/hỗ trợ (mặc định chọn sẵn theo phân công), danh sách tham gia lấy từ check-in; sau khi chốt, hệ thống cộng điểm hàng loạt và khóa sự kiện.
- Mức điểm cấu hình được; điểm đã phát không thay đổi khi đổi quy tắc.

### 11.2. Kỳ hoạt động & reset điểm
- Điểm được tính theo **kỳ**. Sang kỳ mới, số dư điểm dùng để đổi quà trở về 0.
- Hệ thống vẫn ghi nhận **tổng điểm trọn đời** của mỗi thành viên (chỉ tính điểm kiếm được, không trừ điểm đã đổi quà).
- HR/BCN tạo kỳ hoạt động, thiết lập thời gian kỳ và thời gian mở shop đổi quà.

### 11.3. Member
- Xem số dư điểm kỳ hiện tại và tổng điểm trọn đời.
- Xem lịch sử cộng/trừ điểm (nguồn: sự kiện nào, vai trò gì, đổi quà gì).
- Xem các phần quà đang trưng bày trong shop (hình ảnh, mô tả, số điểm, số lượng còn lại).
- Đổi điểm để đăng ký nhận quà trong thời gian shop mở.
- Hủy đăng ký nhận quà (được hoàn điểm) khi shop còn mở.
- Xem danh sách quà đã đăng ký và trạng thái nhận quà.
- Khiếu nại khi điểm bị ghi nhận sai.

### 11.4. HR / BCN
- Tạo, chỉnh sửa, ẩn phần quà: tên, mô tả, hình ảnh, số điểm, số lượng, giới hạn mỗi người.
- Mở/đóng shop theo kỳ.
- Xem danh sách đăng ký nhận quà, xuất danh sách để chuẩn bị quà.
- Đánh dấu đã trao quà tại buổi tổng kết.
- Điều chỉnh điểm thủ công (cộng/trừ) kèm lý do bắt buộc.
- Xem bảng xếp hạng điểm theo kỳ và trọn đời.

### 11.5. Ràng buộc
- Không cho phép số dư điểm âm; không cho đổi quá số lượng quà còn lại (kể cả khi nhiều người đổi cùng lúc).
- Mọi thay đổi điểm đều được lưu lịch sử, không xóa/sửa giao dịch đã ghi; sửa sai bằng giao dịch điều chỉnh.

Trạng thái đăng ký nhận quà: `RESERVED → DELIVERED`, hoặc `CANCELLED` (hoàn điểm).

---

## 12. Đăng ký sự kiện, Check-in & Thanh toán *(bổ sung 09/10/2026)*

### 12.1. Đăng sự kiện cho đăng ký
- HR và BCN đăng sự kiện đang mở lên web để mọi người đăng ký.
- Phạm vi sự kiện:
  - **Nội bộ**: chỉ thành viên CLB (đã đăng nhập) được đăng ký.
  - **Công khai**: cả người ngoài CLB được đăng ký, không cần tài khoản.
- Khi tạo sự kiện, HR/BCN có thể thêm hình ảnh thu hút (banner, ảnh giới thiệu).
- Thiết lập: miễn phí / có phí (giá vé), số lượng tối đa, thời gian mở/đóng đăng ký.
- Hệ thống tự sinh link và mã QR quảng bá của sự kiện để in poster/đăng mạng xã hội.

### 12.2. Đăng ký
- **Sự kiện miễn phí**: đăng ký là hoàn tất, không cần QR.
- **Sự kiện có phí**: sau khi đăng ký, hệ thống hiển thị **mã QR thanh toán** (chuyển khoản ngân hàng). Khi nhận được tiền, hệ thống tự động ghi nhận đăng ký thành công và gửi email xác nhận.
  - Chỗ được giữ trong thời gian chờ thanh toán; quá hạn thì hủy giữ chỗ.
  - Giao dịch sai số tiền/sai nội dung được đưa vào danh sách để HR đối soát thủ công.
- Người ngoài CLB đăng ký bằng: họ tên, email, số điện thoại, MSSV/trường.
- Không cho đăng ký vượt số lượng tối đa.
- Người đăng ký có thể hủy đăng ký trước hạn.

Trạng thái đăng ký: `PENDING_PAYMENT (chỉ sự kiện có phí) → CONFIRMED → CHECKED_IN`, hoặc `CANCELLED` / `EXPIRED` / `NO_SHOW`.

### 12.3. Check-in (ghi nhận tham gia)
Check-in tách biệt với đăng ký/thanh toán và là căn cứ để cộng điểm tham gia.
- **Thành viên**: quét mã QR check-in hiển thị tại sự kiện bằng điện thoại (đã đăng nhập). Mã thay đổi liên tục để tránh gửi ảnh cho người không có mặt.
- **Người ngoài**: nhận vé QR cá nhân qua email, staff quét tại cửa.
- **Dự phòng**: HR tìm theo tên/MSSV và đánh dấu thủ công.
- HR/BCN xem số người đã check-in theo thời gian thực.
