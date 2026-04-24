import http from "./http";

export async function listUsers() {
  const res = await http.get("/api/users");
  return res.data;
}

export async function createUser(payload) {
  const res = await http.post("/api/users", payload);
  return res.data;
}

