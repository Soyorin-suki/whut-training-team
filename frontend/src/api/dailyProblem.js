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

export async function getTodayProblem(tokens) {
  const res = await http.get("/api/daily-problem/today", {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function checkInToday(submissionId, tokens) {
  const res = await http.post(
    "/api/daily-problem/check-in",
    { submissionId },
    { headers: authHeaders(tokens) }
  );
  return res.data;
}

export async function getDailyHistory(tokens, limit = 14) {
  const res = await http.get("/api/daily-problem/history", {
    headers: authHeaders(tokens),
    params: { limit }
  });
  return res.data;
}

export async function drawPracticeProblem(payload, tokens) {
  const res = await http.post("/api/practice/draw", payload ?? {}, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function checkPractice(drawId, submissionId, tokens) {
  const res = await http.post(
    "/api/practice/check",
    { drawId, submissionId },
    { headers: authHeaders(tokens) }
  );
  return res.data;
}

export async function regenerateTodayByAdmin(tokens) {
  const res = await http.post(
    "/api/admin/daily-problem/regenerate",
    {},
    { headers: authHeaders(tokens) }
  );
  return res.data;
}
