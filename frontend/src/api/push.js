import http from "./http";
import { authHeaders } from "./auth";

export async function submitPush(title, link, description) {
  const res = await http.post("/api/push", { title, link, description }, { headers: authHeaders() });
  return res.data;
}

/** 获取已推送的题目列表（普通用户）或全部推题（管理员） */
export async function listPush() {
  const res = await http.get("/api/push", { headers: authHeaders() });
  return res.data;
}

/** 获取今日推题 */
export async function getTodayPush() {
  const res = await http.get("/api/push/today", { headers: authHeaders() });
  return res.data;
}

/** 获取我的推题（当前用户提交的全部推题） */
export async function getMyPushes() {
  const res = await http.get("/api/push/mine", { headers: authHeaders() });
  return res.data;
}

export async function submitPushSolution(pushId, submissionLink, resultDescription) {
  const res = await http.post(
    `/api/push/${pushId}/submit`,
    { submissionLink, resultDescription },
    { headers: authHeaders() }
  );
  return res.data;
}

export async function getPushSubmissions(pushId) {
  const res = await http.get(`/api/push/${pushId}/submissions`, { headers: authHeaders() });
  return res.data;
}

export async function approvePushItem(pushId) {
  const res = await http.post(`/api/admin/push/${pushId}/approve`, {}, { headers: authHeaders() });
  return res.data;
}

export async function rejectPushItem(pushId) {
  const res = await http.post(`/api/admin/push/${pushId}/reject`, {}, { headers: authHeaders() });
  return res.data;
}

export async function promotePushItem(pushId) {
  const res = await http.post(`/api/admin/push/${pushId}/promote`, {}, { headers: authHeaders() });
  return res.data;
}

export async function deletePushItem(pushId) {
  const res = await http.delete(`/api/admin/push/${pushId}`, { headers: authHeaders() });
  return res.data;
}

export async function getPushHistory() {
  const res = await http.get("/api/push/history", { headers: authHeaders() });
  return res.data;
}

export async function getPushPool() {
  const res = await http.get("/api/admin/push/pool", { headers: authHeaders() });
  return res.data;
}
