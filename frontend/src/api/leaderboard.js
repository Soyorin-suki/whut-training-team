import http from "./http";
import { authHeaders } from "./auth";

export async function getLeaderboard(limit = 10, page = 1) {
  const res = await http.get("/api/leaderboard", {
    headers: authHeaders(),
    params: { limit, page, type: "total" },
  });
  return res.data;
}
