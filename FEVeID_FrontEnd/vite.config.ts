import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Request bắt đầu bằng /api → Vite chuyển tiếp sang Spring Boot.
      // Trình duyệt chỉ thấy localhost:5173 nên cookie access_token/XSRF-TOKEN coi như cùng domain với trang (không cần CORS).
      "/api": "http://localhost:8080",
    },
  },
});
