import http from "./http";
import { authHeaders } from "./auth";

export async function getHomeOverview(top = 10) {
  const res = await http.get("/api/home", {
    headers: authHeaders(),
    params: { top },
  });
  // HomeController returns data directly, NOT wrapped in ApiResponse
  return res.data;
}
