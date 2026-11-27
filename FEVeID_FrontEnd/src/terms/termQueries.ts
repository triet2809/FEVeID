import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { TermResponse } from "../api/types";

// GET /api/terms (TermController): mọi kỳ, mới nhất trước. Trang "Kỳ học" sau này dùng lại
export function fetchTerms(): Promise<TermResponse[]> {
  return api.get<TermResponse[]>("/api/terms");
}

export const TERMS_KEY = ["terms"] as const;

export function useTerms() {
  return useQuery({ queryKey: TERMS_KEY, queryFn: fetchTerms });
}
