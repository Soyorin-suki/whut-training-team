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

export async function getAdminTrainingOverview(tokens) {
  const res = await http.get("/api/admin/training/overview", {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getAdminDailyRecords(params, tokens) {
  const res = await http.get("/api/admin/training/daily-records", {
    headers: authHeaders(tokens),
    params
  });
  return res.data;
}

export async function getAdminDailyRecordDetail(date, tokens) {
  const res = await http.get(`/api/admin/training/daily-records/${date}`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getAdminUserTrainingPage(params, tokens) {
  const res = await http.get("/api/admin/training/users", {
    headers: authHeaders(tokens),
    params
  });
  return res.data;
}

export async function getAdminUserTimeline(userId, params, tokens) {
  const res = await http.get(`/api/admin/training/users/${userId}/timeline`, {
    headers: authHeaders(tokens),
    params
  });
  return res.data;
}
