import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { errorMessage } from "../api/client";
import type { PointAdjustmentRequest } from "../api/types";
import { adjustPoints, fetchMemberPoints, fetchMyPoints } from "./pointApi";

// ["points", "me"] và ["points", 12] cùng bắt đầu bằng "points":
// invalidate ["points"] là làm mới cả điểm của tôi lẫn điểm của mọi thành viên đang có trong cache
export const POINTS_KEY = ["points"] as const;
export const MY_POINTS_KEY = ["points", "me"] as const;
export const memberPointsKey = (memberId: number) =>
  ["points", memberId] as const;

export function useMyPoints() {
  return useQuery({ queryKey: MY_POINTS_KEY, queryFn: fetchMyPoints });
}

// memberId undefined = HR chưa chọn ai → chưa gọi API (enabled)
export function useMemberPoints(memberId: number | undefined) {
  return useQuery({
    queryKey: memberPointsKey(memberId ?? 0),
    queryFn: () => fetchMemberPoints(memberId!),
    enabled: memberId !== undefined,
  });
}

// Điều chỉnh xong: số dư + lịch sử đã cũ → làm mới mọi khóa "points"
// (gồm cả "điểm của tôi" nếu HR tự điều chỉnh cho chính mình)
export function useAdjustPoints(memberId: number, onDone: () => void) {
  const queryClient = useQueryClient();
  const { message } = App.useApp();

  return useMutation({
    mutationFn: (request: PointAdjustmentRequest) =>
      adjustPoints(memberId, request),
    onSuccess: (transaction) => {
      queryClient.invalidateQueries({ queryKey: POINTS_KEY });
      message.success(
        `Đã ${transaction.amount > 0 ? "cộng" : "trừ"} ${Math.abs(transaction.amount)} điểm (${transaction.termName})`,
      );
      onDone();
    },
    // vd 409 "Không đủ điểm: số dư của kỳ không được âm"
    onError: (err) => message.error(errorMessage(err)),
  });
}
