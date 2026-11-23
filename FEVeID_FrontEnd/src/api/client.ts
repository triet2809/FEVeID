// Một chỗ duy nhất gọi backend. Mọi hàm API khác (getMe, getTerms...) đều đi qua apiFetch,
// giống mọi request vào Spring đều đi qua chuỗi filter: thêm/sửa luật ở đây là cả app đổi theo.

// Lỗi có kèm HTTP status (giống một RuntimeException có field status bên Java)
export class HttpError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function isUnauthorized(err: unknown): boolean {
  return err instanceof HttpError && err.status === 401;
}

export function errorMessage(err: unknown): string {
  return err instanceof HttpError ? err.message : "Không kết nối được máy chủ";
}

// Các method sửa dữ liệu → backend bắt gửi header CSRF (GET thì không cần, xem SecurityConfig)
const UNSAFE_METHODS = ["POST", "PUT", "PATCH", "DELETE"];
const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

// TODO(9.3 - bạn viết): trả về giá trị cookie tên `name`, không có thì trả undefined.
// Gợi ý:
// - document.cookie là MỘT chuỗi dạng "a=1; XSRF-TOKEN=abc123; b=2" (chỉ chứa cookie KHÔNG httpOnly,
//   nên access_token không bao giờ xuất hiện ở đây, đúng như mong muốn)
// - tách theo "; " → tìm phần tử bắt đầu bằng `${name}=` → lấy phần sau dấu "=" đầu tiên
// - giá trị cookie có thể bị mã hóa URL → decodeURIComponent(...)
// - hàm mảng hay dùng: split, find, startsWith, slice
function readCookie(name: string): string | undefined {
  const prefix = `${name}=`; // "XSRF-TOKEN="
  // document.cookie = "a=1; XSRF-TOKEN=abc%3D123; b=2"
  const found = document.cookie
    .split("; ") // → ["a=1", "XSRF-TOKEN=abc%3D123", "b=2"]
    .find((part) => part.startsWith(prefix)); // → "XSRF-TOKEN=abc%3D123", không thấy → undefined
  if (found === undefined) {
    return undefined;
  }
  // Bỏ phần "XSRF-TOKEN=" ở đầu → "abc%3D123", giải mã URL → "abc=123"
  return decodeURIComponent(found.slice(prefix.length));
}

// Backend trả lỗi dạng ApiError { status, error, message, path } → lấy message; không đọc được thì dùng câu chung
async function readErrorMessage(res: Response): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string };
    if (body.message) return body.message;
  } catch {
    // body rỗng (401/403 do Spring Security trả) hoặc không phải JSON → dùng câu mặc định bên dưới
  }
  if (res.status === 401)
    return "Phiên đăng nhập đã hết, vui lòng đăng nhập lại";
  if (res.status === 403) return "Bạn không có quyền thực hiện thao tác này";
  return `Lỗi máy chủ (HTTP ${res.status})`;
}

async function apiFetch<T>(
  path: string,
  method: string,
  body?: unknown,
): Promise<T> {
  const headers = new Headers();
  if (body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  // Chỉ các method sửa dữ liệu mới cần (GET được backend bỏ qua CSRF)
  if (UNSAFE_METHODS.includes(method)) {
    const csrfToken = readCookie(CSRF_COOKIE);
    // Chưa có cookie (vd: lần đầu gọi login) thì không gắn; nếu không, header sẽ thành chuỗi "undefined"
    if (csrfToken !== undefined) {
      headers.set(CSRF_HEADER, csrfToken);
    }
  }
  const res = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    // Gửi kèm cookie (access_token, XSRF-TOKEN). "same-origin" là mặc định, ghi ra cho rõ ý:
    // chỉ gửi cookie khi gọi cùng domain với trang → khi deploy, frontend và /api phải chung domain.
    credentials: "same-origin",
  });

  if (!res.ok) {
    throw new HttpError(res.status, await readErrorMessage(res));
  }
  if (res.status === 204) {
    return undefined as T; // 204 No Content: không có body để đọc (vd: login, logout)
  }
  return (await res.json()) as T;
}

// Đường dẫn tương đối "/api/..." → lúc dev đi qua Vite proxy, lúc deploy đi qua nginx
export const api = {
  get: <T>(path: string) => apiFetch<T>(path, "GET"),
  post: <T>(path: string, body?: unknown) => apiFetch<T>(path, "POST", body),
  put: <T>(path: string, body?: unknown) => apiFetch<T>(path, "PUT", body),
  patch: <T>(path: string, body?: unknown) => apiFetch<T>(path, "PATCH", body),
  delete: <T>(path: string) => apiFetch<T>(path, "DELETE"),
};
