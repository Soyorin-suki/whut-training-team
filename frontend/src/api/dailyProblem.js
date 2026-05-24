import http from "./http";
import { authHeaders } from "./auth";

export async function getTodayProblem() {
  const res = await http.get("/api/daily-problem/today", { headers: authHeaders() });
  return res.data;
}

export async function checkInToday(submissionId) {
  const res = await http.post(
    "/api/daily-problem/check-in",
    { submissionId },
    { headers: authHeaders() }
  );
  return res.data;
}

export async function getDailyHistory(days = 0) {
  const res = await http.get("/api/daily-problem/history", {
    headers: authHeaders(),
    params: { days },
  });
  return res.data;
}

export async function drawPracticeProblem(minRating, maxRating) {
  const payload = {};
  if (minRating != null) payload.minRating = Number(minRating);
  if (maxRating != null) payload.maxRating = Number(maxRating);
  const res = await http.post("/api/practice/draw", payload, {
    headers: authHeaders(),
  });
  return res.data;
}

export async function checkPractice(drawId, submissionId) {
  const res = await http.post(
    "/api/practice/check",
    { drawId, submissionId },
    { headers: authHeaders() }
  );
  return res.data;
}

export async function getPracticeHistory(limit = 20) {
  const res = await http.get("/api/practice/history", {
    headers: authHeaders(),
    params: { limit },
  });
  return res.data;
}

export async function deletePracticeDraw(drawId) {
  const res = await http.delete(`/api/practice/${drawId}`, {
    headers: authHeaders(),
  });
  return res.data;
}
