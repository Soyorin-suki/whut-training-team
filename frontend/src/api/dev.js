import http from "./http";
import { authHeaders } from "./auth";

/** 检查后端是否处于 dev 模式（dev profile 下才可用） */
export async function checkDevStatus() {
  const res = await http.get("/api/dev/status", { headers: authHeaders() });
  return res.data;
}

/** 获取当前开发时间设置 */
export async function getDevTime() {
  const res = await http.get("/api/dev/time", { headers: authHeaders() });
  return res.data;
}

/** 设置开发环境时间 */
export async function setDevTime(dateTime) {
  const res = await http.post("/api/dev/time", { dateTime }, { headers: authHeaders() });
  return res.data;
}

/** 重置开发环境时间为系统时间 */
export async function resetDevTime() {
  const res = await http.post("/api/dev/time/reset", {}, { headers: authHeaders() });
  return res.data;
}

/** 强制打卡（跳过CF校验） */
export async function forceDevCheckIn(date, slot, userId) {
  const body = {};
  if (date) body.date = date;
  if (slot) body.slot = slot;
  if (userId) body.userId = String(userId);
  const res = await http.post("/api/dev/check-in", body, { headers: authHeaders() });
  return res.data;
}
