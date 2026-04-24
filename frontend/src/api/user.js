import http from "./http";

function authHeaders(tokens) {
  if (!tokens?.accessToken || !tokens?.refreshToken) {
    return {};
  }
  return {
    Authorization: `Bearer ${tokens.accessToken}`,
    "X-Refresh-Token": tokens.refreshToken
  };
}

export async function listUsers(tokens) {
  const res = await http.get("/api/users", {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function registerUser(payload) {
  const res = await http.post("/api/users/register", payload);
  return res.data;
}

export async function login(payload) {
  const res = await http.post("/api/auth/login", payload);
  return res.data;
}

export async function logout(tokens) {
  const res = await http.post(
    "/api/auth/logout",
    {},
    {
      headers: authHeaders(tokens)
    }
  );
  return res.data;
}

export async function adminCreateUser(payload, tokens) {
  const res = await http.post(
    "/api/admin/users",
    payload,
    {
      headers: authHeaders(tokens)
    }
  );
  return res.data;
}
