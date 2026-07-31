import http from "./http";
import { authHeaders } from "./auth";

export async function getUpcomingContests() {
  const res = await http.get("/api/contests/upcoming", {
    headers: authHeaders(),
  });
  return res.data;
}
