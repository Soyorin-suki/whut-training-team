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

export async function getLeaderboard(params, tokens) {
  const res = await http.get("/api/rankings", {
    headers: authHeaders(tokens),
    params
  });
  return res.data;
}

export async function getMyLeaderboardRank(type, tokens) {
  const res = await http.get("/api/rankings/me", {
    headers: authHeaders(tokens),
    params: { type }
  });
  return res.data;
}
