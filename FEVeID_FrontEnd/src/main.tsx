import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import { QueryCache, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { App as AntApp, ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";
import { HttpError, isUnauthorized } from "./api/client";
import { ME_QUERY_KEY } from "./auth/useMe";
import { appTheme } from "./theme";
import "./index.css";
import App from "./App.tsx";

// QueryClient = "kho" chứa mọi dữ liệu đã tải từ backend (cache). Cả app dùng chung MỘT kho.
const queryClient: QueryClient = new QueryClient({
  // Cookie hết hạn (sau 24h) khi đang ở một trang bất kỳ → API của trang đó trả 401.
  // Khi đó xóa kết quả /me cũ → useMe gọi lại /me → 401 → RequireAuth đưa về /login.
  // (Bỏ qua chính query /me, nếu không sẽ lặp vô hạn: /me 401 → xóa /me → gọi lại → 401...)
  queryCache: new QueryCache({
    onError: (error, query) => {
      if (isUnauthorized(error) && query.queryKey[0] !== ME_QUERY_KEY[0]) {
        queryClient.resetQueries({ queryKey: ME_QUERY_KEY });
      }
    },
  }),
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
