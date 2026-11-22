import type { ThemeConfig } from "antd";

// Theme của cả app: đổi ở đây → mọi nút, link, bảng... đổi theo.
// Màu chủ đạo #7E00B2 là tạm (đo từ ảnh áo CLB) → thay bằng mã gốc khi có từ người thiết kế.
export const appTheme: ThemeConfig = {
  token: {
    colorPrimary: "#7E00B2",
    borderRadius: 8,
    fontFamily: "'Be Vietnam Pro', system-ui, sans-serif",
  },
  components: {
    Layout: {
      headerBg: "#ffffff",
      bodyBg: "#f5f7f6",
    },
  },
};
