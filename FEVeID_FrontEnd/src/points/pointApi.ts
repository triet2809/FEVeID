import { api } from "../api/client";
import type {
  PointAdjustmentRequest,
  PointSummaryResponse,
  PointTransactionResponse,
} from "../api/types";

// Mỗi hàm = một endpoint của PointController bên backend

export function fetchMyPoints(): Promise<PointSummaryResponse> {
  return api.get<PointSummaryResponse>("/api/points/me");
}

// Chỉ HR/BCN
export function fetchMemberPoints(
  memberId: number,
): Promise<PointSummaryResponse> {
  return api.get<PointSummaryResponse>(`/api/members/${memberId}/points`);
}

// Chỉ HR/BCN. termId bỏ trống = kỳ hiện tại
export function adjustPoints(
  memberId: number,
  request: PointAdjustmentRequest,
): Promise<PointTransactionResponse> {
  return api.post<PointTransactionResponse>(
    `/api/members/${memberId}/points/adjustments`,
    request,
  );
}
