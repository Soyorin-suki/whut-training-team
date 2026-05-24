import http from "./http";
import { authHeaders } from "./auth";

export async function registerUser(payload) {
  const res = await http.post("/api/users/register", payload);
  return res.data;
}

export async function listUsers() {
  const res = await http.get("/api/users", { headers: authHeaders() });
  return res.data;
}

export async function getUserById(id) {
  const res = await http.get(`/api/users/${id}`, { headers: authHeaders() });
  return res.data;
}

export async function updateMyProfile(id, payload) {
  const res = await http.patch(`/api/users/${id}`, payload ?? {}, {
    headers: authHeaders(),
  });
  return res.data;
}

export async function getHeatmap(userId, days = 180) {
  const res = await http.get(`/api/users/${userId}/daily-heatmap`, {
    headers: authHeaders(),
    params: { days },
  });
  return res.data;
}
