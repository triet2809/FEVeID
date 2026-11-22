import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { App as AntApp, ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";
import { HttpError } from "./api/client";
import { appTheme } from "./theme";
import "./index.css";
import App from "./App.tsx";

// QueryClient = "kho" chứa mọi dữ liệu đã tải từ backend (cache). Cả app dùng chung MỘT kho.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60_000, // trong 5 phút quay lại trang thì dùng cache, không gọi API lại
      // Lỗi 4xx (401, 403, 404) thử lại cũng vô ích; lỗi mạng/5xx thử thêm tối đa 2 lần
      retry: (failureCount, error) => {
        if (error instanceof HttpError && error.status < 500) return false;
        return failureCount < 2;
      },
    },
  },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {/* ConfigProvider: theme (màu, font) + tiếng Việt cho mọi component Ant Design */}
    <ConfigProvider theme={appTheme} locale={viVN}>
      {/* AntApp: cho phép dùng message/notification ở bất kỳ component nào */}
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
          <ReactQueryDevtools />
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  </StrictMode>,
);
