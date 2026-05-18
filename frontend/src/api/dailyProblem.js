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

export async function getProblemDetail(problemKey, tokens) {
  const res = await http.get(`/api/problems/${encodeURIComponent(problemKey)}`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getProblemComments(problemKey, tokens) {
  const res = await http.get(`/api/problem-comments/${encodeURIComponent(problemKey)}`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function createProblemComment(payload, tokens) {
  const res = await http.post("/api/problem-comments", payload ?? {}, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getTodayComments(tokens) {
  const res = await http.get("/api/daily-problem/comments/today", {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getDailyCommentsByInstance(date, problemKey, tokens) {
  const res = await http.get("/api/daily-problem/comments", {
    headers: authHeaders(tokens),
    params: { date, problemKey }
  });
  return res.data;
}

export async function getDailyCommentArchives(tokens, limit = 30) {
  const res = await http.get("/api/daily-problem/comments/archives", {
    headers: authHeaders(tokens),
    params: { limit }
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

export async function createDailyComment(payload, tokens) {
  const res = await http.post("/api/daily-problem/comments", payload ?? {}, {
    headers: authHeaders(tokens)
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

export async function getPracticeHistory(tokens, limit = 30) {
  const res = await http.get("/api/practice/history", {
    headers: authHeaders(tokens),
    params: { limit }
  });
  return res.data;
}

export async function likeProblem(problemKey, tokens) {
  const res = await http.post(
    "/api/problem-like",
    { problemKey },
    { headers: authHeaders(tokens) }
  );
  return res.data;
}

export async function unlikeProblem(problemKey, tokens) {
  const res = await http.delete(`/api/problem-like/${encodeURIComponent(problemKey)}`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function favoriteProblem(problemKey, tokens) {
  const res = await http.post(
    "/api/problem-favorite",
    { problemKey },
    { headers: authHeaders(tokens) }
  );
  return res.data;
}

export async function unfavoriteProblem(problemKey, tokens) {
  const res = await http.delete(`/api/problem-favorite/${encodeURIComponent(problemKey)}`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getMyFavoriteProblems(tokens, options = {}) {
  const { page = 1, limit = 50 } = options;
  const res = await http.get("/api/problem-favorite/mine", {
    headers: authHeaders(tokens),
    params: { page, limit }
  });
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
