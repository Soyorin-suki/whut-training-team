import http from "./http";
import { authHeaders } from "./auth";

export async function adminCreateUser(payload) {
  const res = await http.post("/api/admin/users", payload, { headers: authHeaders() });
  return res.data;
}

export async function changeUserRole(userId, role) {
  const res = await http.put(
    `/api/admin/users/${userId}/role`,
    { role },
    { headers: authHeaders() }
  );
  return res.data;
}

export async function changeUserMemberType(userId, memberType) {
  const res = await http.put(
    `/api/admin/users/${userId}/member-type`,
    { memberType },
    { headers: authHeaders() }
  );
  return res.data;
}

export async function regenerateDaily() {
  const res = await http.post(
    "/api/admin/daily-problem/regenerate",
    {},
    { headers: authHeaders() }
  );
  return res.data;
}

export async function redrawSlot(date, slot, confirm = false) {
  const params = new URLSearchParams({ slot, confirm: String(confirm) });
  if (date) params.set("date", date);
  const res = await http.post(
    `/api/admin/daily-problem/redraw?${params}`,
    {},
    { headers: authHeaders() }
  );
  return res.data;
}

export async function getRoles() {
  const res = await http.get("/api/roles", { headers: authHeaders() });
  return res.data;
}
