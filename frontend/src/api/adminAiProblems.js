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

export async function listAdminAiProblemSessions(params, tokens) {
  const res = await http.get("/api/admin/ai-problems/sessions", {
    headers: authHeaders(tokens),
    params
  });
  return res.data;
}

export async function createAdminAiProblemSession(payload, tokens) {
  const res = await http.post("/api/admin/ai-problems/sessions", payload, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function getAdminAiProblemSessionDetail(sessionId, tokens) {
  const res = await http.get(`/api/admin/ai-problems/sessions/${sessionId}`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function appendAdminAiProblemMessage(sessionId, payload, tokens) {
  const res = await http.post(`/api/admin/ai-problems/sessions/${sessionId}/messages`, payload, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function activateAdminAiProblemVersion(draftId, versionNo, tokens) {
  const res = await http.post(
    `/api/admin/ai-problems/drafts/${draftId}/versions/${versionNo}/activate`,
    {},
    {
      headers: authHeaders(tokens)
    }
  );
  return res.data;
}

export async function patchAdminAiProblemDraft(draftId, payload, tokens) {
  const res = await http.patch(`/api/admin/ai-problems/drafts/${draftId}`, payload, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function regenerateAdminAiProblemArtifacts(draftId, tokens) {
  const res = await http.post(
    `/api/admin/ai-problems/drafts/${draftId}/artifacts/regenerate`,
    {},
    {
      headers: authHeaders(tokens)
    }
  );
  return res.data;
}

export async function getAdminAiProblemArtifacts(draftId, tokens) {
  const res = await http.get(`/api/admin/ai-problems/drafts/${draftId}/artifacts`, {
    headers: authHeaders(tokens)
  });
  return res.data;
}

export async function downloadAdminAiProblemZip(draftId, tokens) {
  return http.get(`/api/admin/ai-problems/drafts/${draftId}/artifacts/download`, {
    headers: authHeaders(tokens),
    responseType: "blob"
  });
}
