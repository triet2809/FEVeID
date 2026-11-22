import { Route, Routes } from "react-router";
import { HomePage } from "./pages/HomePage";
import { NotFoundPage } from "./pages/NotFoundPage";

// Bảng định tuyến của cả app (giống các @GetMapping, nhưng chọn TRANG để vẽ thay vì method để chạy).
// 9.4 sẽ thêm: /login, khu nội bộ bọc trong RequireAuth + AppLayout, trang riêng theo role.
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
