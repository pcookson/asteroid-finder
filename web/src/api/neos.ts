import type { NeoSummary } from "../types";

export async function fetchTodayNeos(): Promise<NeoSummary[]> {
  const response = await fetch("/api/neos/today");

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const data = (await response.json()) as unknown;
  if (!Array.isArray(data)) {
    throw new Error("Unexpected response shape");
  }

  return data as NeoSummary[];
}

