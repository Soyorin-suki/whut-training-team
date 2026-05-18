import { useEffect, useMemo, useState } from "react";
import {
  activateAdminAiProblemVersion,
  appendAdminAiProblemMessage,
  createAdminAiProblemSession,
  downloadAdminAiProblemZip,
  getAdminAiProblemSessionDetail,
  listAdminAiProblemSessions,
  patchAdminAiProblemDraft,
  regenerateAdminAiProblemArtifacts
} from "../api/adminAiProblems";

function formatDateTimeLabel(value) {
  if (!value) {
    return "-";
  }
  try {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    }).format(date);
  } catch {
    return value;
  }
}

function splitTags(rawValue) {
  return String(rawValue || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function emptyDraftForm() {
  return {
    title: "",
    statementMd: "",
    inputSpecMd: "",
    outputSpecMd: "",
    constraintMd: "",
    hintMd: "",
    rating: "",
    tags: "",
    checkerNoteMd: "",
    testPlanMd: "",
    originalityNotice: "",
    samples: [],
    tests: []
  };
}

function toDraftForm(draft) {
  if (!draft) {
    return emptyDraftForm();
  }
  return {
    title: draft.title || "",
    statementMd: draft.statementMd || "",
    inputSpecMd: draft.inputSpecMd || "",
    outputSpecMd: draft.outputSpecMd || "",
    constraintMd: draft.constraintMd || "",
    hintMd: draft.hintMd || "",
    rating: draft.rating == null ? "" : String(draft.rating),
    tags: Array.isArray(draft.tags) ? draft.tags.join(", ") : "",
    checkerNoteMd: draft.checkerNoteMd || "",
    testPlanMd: draft.testPlanMd || "",
    originalityNotice: draft.originalityNotice || "",
    samples: Array.isArray(draft.samples)
      ? draft.samples.map((item) => ({
          input: item.input || "",
          output: item.output || "",
          explanation: item.explanation || ""
        }))
      : [],
    tests: Array.isArray(draft.tests)
      ? draft.tests.map((item) => ({
          name: item.name || "",
          input: item.input || "",
          output: item.output || ""
        }))
      : []
  };
}

function readResponseMessage(payload, fallbackText) {
  if (payload?.message) {
    return payload.message;
  }
  return fallbackText;
}

function parseFileName(disposition, fallbackName) {
  if (!disposition) {
    return fallbackName;
  }
  const match = disposition.match(/filename="?([^"]+)"?/i);
  return match?.[1] || fallbackName;
}

export default function AdminAiProblemWorkbench({ auth }) {
  const tokens = auth?.tokens ?? null;
  const [filters, setFilters] = useState({
    keyword: "",
    status: "",
    page: 1,
    pageSize: 8
  });
  const [sessionsState, setSessionsState] = useState({
    loading: false,
    message: "",
    total: 0,
    entries: []
  });
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [detailState, setDetailState] = useState({
    loading: false,
    message: "",
    data: null
  });
  const [createForm, setCreateForm] = useState({
    providerKey: "",
    targetRating: "1600",
    targetTags: "dp, graphs",
    problemStyle: "",
    extraRequirements: ""
  });
  const [chatInput, setChatInput] = useState("");
  const [draftForm, setDraftForm] = useState(emptyDraftForm());
  const [selectedArtifactPath, setSelectedArtifactPath] = useState("");
  const [actionBusy, setActionBusy] = useState("");
  const [actionMessage, setActionMessage] = useState("");

  const artifactItems = detailState.data?.artifactBundle?.items || [];
  const selectedArtifact = useMemo(
    () => artifactItems.find((item) => item.relativePath === selectedArtifactPath) || artifactItems[0] || null,
    [artifactItems, selectedArtifactPath]
  );
  const sessionPageCount = sessionsState.total > 0 ? Math.ceil(sessionsState.total / filters.pageSize) : 1;

  useEffect(() => {
    if (!tokens) {
      return;
    }
    void reloadSessions(1);
  }, [tokens]);

  useEffect(() => {
    setDraftForm(toDraftForm(detailState.data?.draft));
    if (!artifactItems.some((item) => item.relativePath === selectedArtifactPath)) {
      setSelectedArtifactPath(artifactItems[0]?.relativePath || "");
    }
  }, [detailState.data]);

  async function reloadSessions(nextPage = filters.page) {
    if (!tokens) {
      return;
    }
    setSessionsState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));
    try {
      const resp = await listAdminAiProblemSessions(
        {
          keyword: filters.keyword || undefined,
          status: filters.status || undefined,
          page: nextPage,
          pageSize: filters.pageSize
        },
        tokens
      );
      if (resp.code !== 200) {
        setSessionsState((current) => ({
          ...current,
          loading: false,
          message: readResponseMessage(resp, "加载会话列表失败")
        }));
        return;
      }
      const entries = resp.data?.entries || [];
      const nextSelectedId = entries.some((item) => item.sessionId === selectedSessionId)
        ? selectedSessionId
        : entries[0]?.sessionId || null;

      setFilters((current) => ({
        ...current,
        page: resp.data?.page || nextPage
      }));
      setSessionsState({
        loading: false,
        message: "",
        total: resp.data?.total || 0,
        entries
      });

      if (nextSelectedId) {
        setSelectedSessionId(nextSelectedId);
        await loadSessionDetail(nextSelectedId);
      } else {
        setSelectedSessionId(null);
        setDetailState({
          loading: false,
          message: "",
          data: null
        });
      }
    } catch (error) {
      setSessionsState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载会话列表失败"
      }));
    }
  }

  async function loadSessionDetail(sessionId) {
    if (!tokens || !sessionId) {
      return;
    }
    setDetailState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));
    try {
      const resp = await getAdminAiProblemSessionDetail(sessionId, tokens);
      if (resp.code !== 200) {
        setDetailState((current) => ({
          ...current,
          loading: false,
          message: readResponseMessage(resp, "加载会话详情失败")
        }));
        return;
      }
      setDetailState({
        loading: false,
        message: "",
        data: resp.data || null
      });
    } catch (error) {
      setDetailState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载会话详情失败"
      }));
    }
  }

  function applySessionDetail(detail, message) {
    setDetailState({
      loading: false,
      message: "",
      data: detail || null
    });
    setSelectedSessionId(detail?.session?.sessionId || null);
    setActionMessage(message || "");
  }

  async function handleCreateSession() {
    if (!tokens || actionBusy) {
      return;
    }
    const payload = {
      providerKey: createForm.providerKey.trim() || undefined,
      targetRating: Number(createForm.targetRating),
      targetTags: splitTags(createForm.targetTags),
      problemStyle: createForm.problemStyle.trim() || undefined,
      extraRequirements: createForm.extraRequirements.trim() || undefined
    };
    setActionBusy("create");
    setActionMessage("");
    try {
      const resp = await createAdminAiProblemSession(payload, tokens);
      if (resp.code !== 200) {
        setActionMessage(readResponseMessage(resp, "创建会话失败"));
        return;
      }
      applySessionDetail(resp.data, "已创建新会话并生成首版草稿。");
      await reloadSessions(1);
    } catch (error) {
      setActionMessage(error.response?.data?.message || "创建会话失败");
    } finally {
      setActionBusy("");
    }
  }

  async function handleSendMessage() {
    if (!tokens || !selectedSessionId || !chatInput.trim() || actionBusy) {
      return;
    }
    setActionBusy("chat");
    setActionMessage("");
    try {
      const resp = await appendAdminAiProblemMessage(
        selectedSessionId,
        { content: chatInput.trim() },
        tokens
      );
      if (resp.code !== 200) {
        setActionMessage(readResponseMessage(resp, "继续生成失败"));
        return;
      }
      setChatInput("");
      applySessionDetail(resp.data, "已生成新版本。");
      await reloadSessions(filters.page);
    } catch (error) {
      setActionMessage(error.response?.data?.message || "继续生成失败");
    } finally {
      setActionBusy("");
    }
  }

  async function handleSaveDraft() {
    const draftId = detailState.data?.draft?.draftId;
    if (!tokens || !draftId || actionBusy) {
      return;
    }
    setActionBusy("save-draft");
    setActionMessage("");
    try {
      const resp = await patchAdminAiProblemDraft(
        draftId,
        {
          title: draftForm.title,
          statementMd: draftForm.statementMd,
          inputSpecMd: draftForm.inputSpecMd,
          outputSpecMd: draftForm.outputSpecMd,
          constraintMd: draftForm.constraintMd,
          hintMd: draftForm.hintMd,
          rating: Number(draftForm.rating),
          tags: splitTags(draftForm.tags),
          checkerNoteMd: draftForm.checkerNoteMd || undefined,
          testPlanMd: draftForm.testPlanMd,
          originalityNotice: draftForm.originalityNotice,
          samples: draftForm.samples.map((item) => ({
            input: item.input,
            output: item.output,
            explanation: item.explanation || undefined
          })),
          tests: draftForm.tests.map((item) => ({
            name: item.name || undefined,
            input: item.input,
            output: item.output
          }))
        },
        tokens
      );
      if (resp.code !== 200) {
        setActionMessage(readResponseMessage(resp, "保存草稿失败"));
        return;
      }
      applySessionDetail(resp.data, "草稿已保存。");
      await reloadSessions(filters.page);
    } catch (error) {
      setActionMessage(error.response?.data?.message || "保存草稿失败");
    } finally {
      setActionBusy("");
    }
  }

  async function handleActivateVersion(versionNo) {
    const draftId = detailState.data?.draft?.draftId;
    if (!tokens || !draftId || actionBusy) {
      return;
    }
    setActionBusy(`activate-${versionNo}`);
    setActionMessage("");
    try {
      const resp = await activateAdminAiProblemVersion(draftId, versionNo, tokens);
      if (resp.code !== 200) {
        setActionMessage(readResponseMessage(resp, "切换版本失败"));
        return;
      }
      applySessionDetail(resp.data, `已切换到 v${versionNo}。`);
      await reloadSessions(filters.page);
    } catch (error) {
      setActionMessage(error.response?.data?.message || "切换版本失败");
    } finally {
      setActionBusy("");
    }
  }

  async function handleRegenerateArtifacts() {
    const draftId = detailState.data?.draft?.draftId;
    if (!tokens || !draftId || actionBusy) {
      return;
    }
    setActionBusy("regenerate-artifacts");
    setActionMessage("");
    try {
      const resp = await regenerateAdminAiProblemArtifacts(draftId, tokens);
      if (resp.code !== 200) {
        setActionMessage(readResponseMessage(resp, "重建产物失败"));
        return;
      }
      setDetailState((current) => ({
        ...current,
        data: current.data
          ? {
              ...current.data,
              artifactBundle: resp.data || current.data.artifactBundle
            }
          : current.data
      }));
      setActionMessage("已按当前草稿重建样例、测试文件和 ZIP。");
    } catch (error) {
      setActionMessage(error.response?.data?.message || "重建产物失败");
    } finally {
      setActionBusy("");
    }
  }

  async function handleDownloadZip() {
    const draftId = detailState.data?.draft?.draftId;
    const versionNo = detailState.data?.draft?.currentVersion;
    if (!tokens || !draftId || !versionNo || actionBusy) {
      return;
    }
    setActionBusy("download-zip");
    setActionMessage("");
    try {
      const response = await downloadAdminAiProblemZip(draftId, tokens);
      const blobUrl = window.URL.createObjectURL(response.data);
      const anchor = document.createElement("a");
      anchor.href = blobUrl;
      anchor.download = parseFileName(
        response.headers?.["content-disposition"],
        `problem-${draftId}-v${versionNo}.zip`
      );
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(blobUrl);
      setActionMessage("题包 ZIP 已开始下载。");
    } catch (error) {
      setActionMessage(error.response?.data?.message || "下载 ZIP 失败");
    } finally {
      setActionBusy("");
    }
  }

  function updateSample(index, field, value) {
    setDraftForm((current) => ({
      ...current,
      samples: current.samples.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item
      )
    }));
  }

  function updateTest(index, field, value) {
    setDraftForm((current) => ({
      ...current,
      tests: current.tests.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item
      )
    }));
  }

  function removeSample(index) {
    setDraftForm((current) => ({
      ...current,
      samples: current.samples.filter((_, itemIndex) => itemIndex !== index)
    }));
  }

  function removeTest(index) {
    setDraftForm((current) => ({
      ...current,
      tests: current.tests.filter((_, itemIndex) => itemIndex !== index)
    }));
  }

  return (
    <section className="section-card admin-ai-workbench">
      <div className="section-heading section-heading-inline">
        <div>
          <p className="section-eyebrow">Admin AI Problem Lab</p>
          <h3>AI 原创出题工作台</h3>
          <p>管理员可以多轮对话生成原创题草稿，保留版本快照，在线调整结构化内容，并导出完整题包。</p>
        </div>
        <div className="admin-ai-toolbar">
          <button
            className="primary-button"
            type="button"
            disabled={Boolean(actionBusy)}
            onClick={handleCreateSession}
          >
            {actionBusy === "create" ? "生成中..." : "新建 AI 会话"}
          </button>
        </div>
      </div>

      {actionMessage ? <p className="system-message">{actionMessage}</p> : null}
      {detailState.message ? <p className="system-message">{detailState.message}</p> : null}

      <div className="admin-ai-grid">
        <div className="admin-ai-column">
          <article className="admin-ai-panel">
            <div className="section-heading">
              <p className="section-eyebrow">Bootstrap</p>
              <h3>会话初始化</h3>
            </div>
            <div className="admin-ai-form-grid">
              <label className="field-stack">
                <span>Provider Key</span>
                <input
                  className="auth-input"
                  value={createForm.providerKey}
                  onChange={(event) =>
                    setCreateForm((current) => ({ ...current, providerKey: event.target.value }))
                  }
                  placeholder="留空时使用默认 provider"
                />
              </label>
              <label className="field-stack">
                <span>Target Rating</span>
                <input
                  className="auth-input"
                  type="number"
                  value={createForm.targetRating}
                  onChange={(event) =>
                    setCreateForm((current) => ({ ...current, targetRating: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack field-span-2">
                <span>Target Tags</span>
                <input
                  className="auth-input"
                  value={createForm.targetTags}
                  onChange={(event) =>
                    setCreateForm((current) => ({ ...current, targetTags: event.target.value }))
                  }
                  placeholder="dp, graphs, greedy"
                />
              </label>
              <label className="field-stack field-span-2">
                <span>Style</span>
                <input
                  className="auth-input"
                  value={createForm.problemStyle}
                  onChange={(event) =>
                    setCreateForm((current) => ({ ...current, problemStyle: event.target.value }))
                  }
                  placeholder="例如：构造题、交互式风格、故事包装"
                />
              </label>
              <label className="field-stack field-span-2">
                <span>Extra Requirements</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={createForm.extraRequirements}
                  onChange={(event) =>
                    setCreateForm((current) => ({
                      ...current,
                      extraRequirements: event.target.value
                    }))
                  }
                  placeholder="例如：限制 2s / 256MB，加入反例样例，题解提示只给方向"
                />
              </label>
            </div>
          </article>

          <article className="admin-ai-panel">
            <div className="section-heading section-heading-inline">
              <div>
                <p className="section-eyebrow">Sessions</p>
                <h3>会话列表</h3>
              </div>
              <div className="admin-filter-row">
                <input
                  className="auth-input"
                  value={filters.keyword}
                  onChange={(event) =>
                    setFilters((current) => ({ ...current, keyword: event.target.value }))
                  }
                  placeholder="按标题或标签搜索"
                />
                <select
                  className="auth-input"
                  value={filters.status}
                  onChange={(event) =>
                    setFilters((current) => ({ ...current, status: event.target.value }))
                  }
                >
                  <option value="">全部状态</option>
                  <option value="READY">READY</option>
                  <option value="PROCESSING">PROCESSING</option>
                  <option value="FAILED">FAILED</option>
                </select>
                <button className="ghost-button" type="button" onClick={() => void reloadSessions(1)}>
                  刷新
                </button>
              </div>
            </div>

            {sessionsState.message ? <p className="system-message">{sessionsState.message}</p> : null}
            {sessionsState.loading ? <p className="system-message is-loading">正在加载会话...</p> : null}

            <div className="admin-ai-session-list">
              {sessionsState.entries.length === 0 ? (
                <p className="empty-copy">当前没有 AI 出题会话。</p>
              ) : (
                sessionsState.entries.map((item) => (
                  <button
                    key={item.sessionId}
                    type="button"
                    className={`admin-ai-session-item ${
                      selectedSessionId === item.sessionId ? "is-active" : ""
                    }`}
                    onClick={() => {
                      setSelectedSessionId(item.sessionId);
                      void loadSessionDetail(item.sessionId);
                    }}
                  >
                    <div>
                      <strong>{item.title || `Session #${item.sessionId}`}</strong>
                      <small>{(item.targetTags || []).join(", ") || "No tags"}</small>
                    </div>
                    <span>
                      v{item.currentVersion || 0}
                      <small>{item.status}</small>
                    </span>
                    <small>{formatDateTimeLabel(item.updatedAt)}</small>
                  </button>
                ))
              )}
            </div>

            <div className="leaderboard-pagination">
              <button
                type="button"
                className="ghost-button"
                disabled={sessionsState.loading || filters.page <= 1}
                onClick={() => void reloadSessions(filters.page - 1)}
              >
                上一页
              </button>
              <span>
                第 {filters.page} / {sessionPageCount} 页
              </span>
              <button
                type="button"
                className="ghost-button"
                disabled={sessionsState.loading || filters.page >= sessionPageCount}
                onClick={() => void reloadSessions(filters.page + 1)}
              >
                下一页
              </button>
            </div>
          </article>
        </div>

        <div className="admin-ai-column">
          <article className="admin-ai-panel">
            <div className="section-heading">
              <p className="section-eyebrow">Conversation</p>
              <h3>多轮对话</h3>
            </div>
            {detailState.loading ? <p className="system-message is-loading">正在加载会话详情...</p> : null}
            <div className="admin-ai-message-list">
              {(detailState.data?.messages || []).length === 0 ? (
                <p className="empty-copy">选择一个会话后，这里会显示管理员和 AI 的完整消息流。</p>
              ) : (
                (detailState.data?.messages || []).map((item) => (
                  <article
                    className={`admin-ai-message is-${item.role || "assistant"}`}
                    key={item.messageId}
                  >
                    <div className="admin-ai-message-head">
                      <strong>{item.role === "user" ? "Admin" : item.role === "assistant" ? "AI" : "System"}</strong>
                      <small>{formatDateTimeLabel(item.createdAt)}</small>
                    </div>
                    <p>{item.content}</p>
                  </article>
                ))
              )}
            </div>

            <div className="admin-ai-composer">
              <textarea
                className="auth-input admin-ai-textarea"
                value={chatInput}
                onChange={(event) => setChatInput(event.target.value)}
                placeholder="例如：保持 rating 1600，但把题意改成图论模型，并补充更强的边界样例。"
              />
              <button
                className="primary-button"
                type="button"
                disabled={!selectedSessionId || !chatInput.trim() || Boolean(actionBusy)}
                onClick={handleSendMessage}
              >
                {actionBusy === "chat" ? "生成中..." : "继续生成"}
              </button>
            </div>
          </article>

          <article className="admin-ai-panel">
            <div className="section-heading">
              <p className="section-eyebrow">Versions</p>
              <h3>版本快照</h3>
            </div>
            <div className="admin-ai-version-list">
              {(detailState.data?.versions || []).length === 0 ? (
                <p className="empty-copy">当前没有版本快照。</p>
              ) : (
                (detailState.data?.versions || []).map((item) => (
                  <div className="admin-ai-version-item" key={item.versionId}>
                    <div>
                      <strong>v{item.versionNo}</strong>
                      <small>{formatDateTimeLabel(item.createdAt)}</small>
                    </div>
                    <p>{item.assistantMessage || "No summary"}</p>
                    <button
                      className={`ghost-button ${item.active ? "is-current" : ""}`}
                      type="button"
                      disabled={item.active || Boolean(actionBusy)}
                      onClick={() => void handleActivateVersion(item.versionNo)}
                    >
                      {item.active ? "当前版本" : "激活为当前"}
                    </button>
                  </div>
                ))
              )}
            </div>
          </article>
        </div>

        <div className="admin-ai-column admin-ai-column-wide">
          <article className="admin-ai-panel">
            <div className="section-heading section-heading-inline">
              <div>
                <p className="section-eyebrow">Draft</p>
                <h3>结构化草稿</h3>
              </div>
              <div className="admin-ai-toolbar">
                <button
                  className="ghost-button"
                  type="button"
                  disabled={!detailState.data?.draft?.draftId || Boolean(actionBusy)}
                  onClick={handleRegenerateArtifacts}
                >
                  {actionBusy === "regenerate-artifacts" ? "重建中..." : "重建产物"}
                </button>
                <button
                  className="ghost-button"
                  type="button"
                  disabled={!detailState.data?.draft?.draftId || Boolean(actionBusy)}
                  onClick={handleDownloadZip}
                >
                  {actionBusy === "download-zip" ? "下载中..." : "下载 ZIP"}
                </button>
                <button
                  className="primary-button"
                  type="button"
                  disabled={!detailState.data?.draft?.draftId || Boolean(actionBusy)}
                  onClick={handleSaveDraft}
                >
                  {actionBusy === "save-draft" ? "保存中..." : "保存草稿"}
                </button>
              </div>
            </div>

            <div className="admin-ai-form-grid">
              <label className="field-stack field-span-2">
                <span>标题</span>
                <input
                  className="auth-input"
                  value={draftForm.title}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, title: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack">
                <span>Rating</span>
                <input
                  className="auth-input"
                  type="number"
                  value={draftForm.rating}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, rating: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack">
                <span>Tags</span>
                <input
                  className="auth-input"
                  value={draftForm.tags}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, tags: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack field-span-2">
                <span>题面 Markdown</span>
                <textarea
                  className="auth-input admin-ai-textarea admin-ai-textarea-lg"
                  value={draftForm.statementMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, statementMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack">
                <span>输入说明</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.inputSpecMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, inputSpecMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack">
                <span>输出说明</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.outputSpecMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, outputSpecMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack">
                <span>约束</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.constraintMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, constraintMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack">
                <span>提示</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.hintMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, hintMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack field-span-2">
                <span>Checker / 审核备注</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.checkerNoteMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, checkerNoteMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack field-span-2">
                <span>测试计划</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.testPlanMd}
                  onChange={(event) =>
                    setDraftForm((current) => ({ ...current, testPlanMd: event.target.value }))
                  }
                />
              </label>
              <label className="field-stack field-span-2">
                <span>原创性提示</span>
                <textarea
                  className="auth-input admin-ai-textarea"
                  value={draftForm.originalityNotice}
                  onChange={(event) =>
                    setDraftForm((current) => ({
                      ...current,
                      originalityNotice: event.target.value
                    }))
                  }
                />
              </label>
            </div>

            <div className="admin-ai-artifact-editors">
              <div className="section-heading section-heading-inline">
                <div>
                  <p className="section-eyebrow">Samples</p>
                  <h3>样例管理</h3>
                </div>
                <button
                  className="ghost-button"
                  type="button"
                  onClick={() =>
                    setDraftForm((current) => ({
                      ...current,
                      samples: [...current.samples, { input: "", output: "", explanation: "" }]
                    }))
                  }
                >
                  添加样例
                </button>
              </div>
              <div className="admin-ai-io-list">
                {draftForm.samples.length === 0 ? (
                  <p className="empty-copy">当前没有样例。</p>
                ) : (
                  draftForm.samples.map((item, index) => (
                    <article className="admin-ai-io-item" key={`sample-${index}`}>
                      <div className="admin-ai-io-head">
                        <strong>样例 {index + 1}</strong>
                        <button className="ghost-button" type="button" onClick={() => removeSample(index)}>
                          删除
                        </button>
                      </div>
                      <div className="admin-ai-split-grid">
                        <label className="field-stack">
                          <span>Input</span>
                          <textarea
                            className="auth-input admin-ai-textarea"
                            value={item.input}
                            onChange={(event) => updateSample(index, "input", event.target.value)}
                          />
                        </label>
                        <label className="field-stack">
                          <span>Output</span>
                          <textarea
                            className="auth-input admin-ai-textarea"
                            value={item.output}
                            onChange={(event) => updateSample(index, "output", event.target.value)}
                          />
                        </label>
                      </div>
                      <label className="field-stack">
                        <span>Explanation</span>
                        <textarea
                          className="auth-input admin-ai-textarea"
                          value={item.explanation}
                          onChange={(event) => updateSample(index, "explanation", event.target.value)}
                        />
                      </label>
                    </article>
                  ))
                )}
              </div>
            </div>

            <div className="admin-ai-artifact-editors">
              <div className="section-heading section-heading-inline">
                <div>
                  <p className="section-eyebrow">Tests</p>
                  <h3>测试文件管理</h3>
                </div>
                <button
                  className="ghost-button"
                  type="button"
                  onClick={() =>
                    setDraftForm((current) => ({
                      ...current,
                      tests: [...current.tests, { name: "", input: "", output: "" }]
                    }))
                  }
                >
                  添加测试
                </button>
              </div>
              <div className="admin-ai-io-list">
                {draftForm.tests.length === 0 ? (
                  <p className="empty-copy">当前没有测试文件。</p>
                ) : (
                  draftForm.tests.map((item, index) => (
                    <article className="admin-ai-io-item" key={`test-${index}`}>
                      <div className="admin-ai-io-head">
                        <strong>测试 {index + 1}</strong>
                        <button className="ghost-button" type="button" onClick={() => removeTest(index)}>
                          删除
                        </button>
                      </div>
                      <label className="field-stack">
                        <span>Name</span>
                        <input
                          className="auth-input"
                          value={item.name}
                          onChange={(event) => updateTest(index, "name", event.target.value)}
                          placeholder="例如：small-random"
                        />
                      </label>
                      <div className="admin-ai-split-grid">
                        <label className="field-stack">
                          <span>Input</span>
                          <textarea
                            className="auth-input admin-ai-textarea"
                            value={item.input}
                            onChange={(event) => updateTest(index, "input", event.target.value)}
                          />
                        </label>
                        <label className="field-stack">
                          <span>Output</span>
                          <textarea
                            className="auth-input admin-ai-textarea"
                            value={item.output}
                            onChange={(event) => updateTest(index, "output", event.target.value)}
                          />
                        </label>
                      </div>
                    </article>
                  ))
                )}
              </div>
            </div>
          </article>

          <article className="admin-ai-panel">
            <div className="section-heading">
              <p className="section-eyebrow">Artifacts</p>
              <h3>产物文件树</h3>
            </div>
            <div className="admin-ai-artifact-grid">
              <div className="admin-ai-file-list">
                {artifactItems.length === 0 ? (
                  <p className="empty-copy">当前没有可预览的产物文件。</p>
                ) : (
                  artifactItems.map((item) => (
                    <button
                      className={`admin-ai-file-item ${
                        selectedArtifact?.relativePath === item.relativePath ? "is-active" : ""
                      }`}
                      key={item.artifactId || item.relativePath}
                      type="button"
                      onClick={() => setSelectedArtifactPath(item.relativePath)}
                    >
                      <strong>{item.fileName}</strong>
                      <small>{item.artifactType}</small>
                      <small>{item.relativePath}</small>
                    </button>
                  ))
                )}
              </div>

              <div className="admin-ai-file-preview">
                {selectedArtifact ? (
                  <>
                    <div className="admin-ai-file-preview-head">
                      <strong>{selectedArtifact.relativePath}</strong>
                      <small>{selectedArtifact.contentType || "-"}</small>
                    </div>
                    {selectedArtifact.contentPreview ? (
                      <pre>{selectedArtifact.contentPreview}</pre>
                    ) : (
                      <p className="empty-copy">该文件不支持在线预览。</p>
                    )}
                  </>
                ) : (
                  <p className="empty-copy">从左侧选择一个文件以查看内容。</p>
                )}
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
