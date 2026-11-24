import { Route, Routes } from "react-router";
import { RequireAuth } from "./auth/RequireAuth";
import { RequireRole } from "./auth/RequireRole";
import { ROLE_GROUPS } from "./auth/roles";
import { AppLayout } from "./layouts/AppLayout";
import { DashboardPage } from "./pages/DashboardPage";
import { EventCreatePage } from "./pages/events/EventCreatePage";
import { EventDetailPage } from "./pages/events/EventDetailPage";
import { EventListPage } from "./pages/events/EventListPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { ProfilePage } from "./pages/ProfilePage";

// Bảng định tuyến (giống các @GetMapping, nhưng chọn TRANG để vẽ). Route lồng nhau = đi qua từng lớp:
//   /app/members → RequireAuth (đã đăng nhập?) → AppLayout (vẽ khung) → RequireRole (đúng role?) → trang
export default function App() {
  return (
    <Routes>
      {/* Khu công khai (Guest) */}
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />

      {/* Khu nội bộ: mọi URL bắt đầu bằng /app phải đăng nhập */}
      <Route path="/app" element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          {/* index = đúng /app */}
          <Route index element={<DashboardPage />} />
          <Route path="profile" element={<ProfilePage />} />

          {/* Chỉ HR và Ban Chủ nhiệm */}
          <Route element={<RequireRole roles={ROLE_GROUPS.memberManagers} />}>
            <Route path="members" element={<PlaceholderPage title="Quản lý thành viên" />} />
            <Route path="terms" element={<PlaceholderPage title="Kỳ học" />} />
          </Route>

          {/* Quản lý sự kiện: HR và Ban Chủ nhiệm. /app/events/new khớp cả "events/new" lẫn "events/:id";
              React Router tự ưu tiên đường dẫn cố định (new) hơn đường dẫn có tham số (:id) */}
          <Route element={<RequireRole roles={ROLE_GROUPS.eventManagers} />}>
            <Route path="events" element={<EventListPage />} />
            <Route path="events/new" element={<EventCreatePage />} />
            <Route path="events/:id" element={<EventDetailPage />} />
          </Route>
        </Route>
      </Route>

      {/* "*" = mọi URL không khớp dòng nào ở trên */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
