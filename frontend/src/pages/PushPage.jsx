import { useEffect, useState } from "react";
import {
  listPush,
  submitPush,
  submitPushSolution,
  getPushSubmissions,
  approvePushItem,
  rejectPushItem,
  promotePushItem,
  getPushHistory,
} from "../api/push";
import { useAuth } from "../context/AuthContext";
import EmptyState from "../components/ui/EmptyState";
import { ListSkeleton } from "../components/ui/Skeleton";
import * as Accordion from "@radix-ui/react-accordion";

export default function PushPage() {
  const { isAdmin } = useAuth();
  const [pushItems, setPushItems] = useState([]);
  const [pushHistory, setPushHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  // Submit push form
  const [title, setTitle] = useState("");
  const [link, setLink] = useState("");
  const [description, setDescription] = useState("");

  // Submit solution modal
  const [solvePushId, setSolvePushId] = useState(null);
  const [solutionLink, setSolutionLink] = useState("");
  const [solutionDesc, setSolutionDesc] = useState("");

  // Submissions view
  const [viewSubmissions, setViewSubmissions] = useState(null);

  const loadPushItems = async () => {
    try {
      const [listResp, histResp] = await Promise.all([
        listPush(),
        getPushHistory(),
      ]);
      if (listResp.code === 200) {
        setPushItems(listResp.data || []);
      }
      if (histResp.code === 200) {
        setPushHistory(histResp.data || []);
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPushItems();
  }, []);

  async function handleSubmitPush() {
    if (!title.trim() || !link.trim()) {
      setMessage("标题和链接不能为空");
      return;
    }
    setMessage("");
    try {
      const resp = await submitPush(title.trim(), link.trim(), description.trim() || null);
      if (resp.code === 200) {
        setTitle("");
        setLink("");
        setDescription("");
        setMessage("推题提交成功");
        loadPushItems();
      } else {
        setMessage(resp.message || "提交失败");
      }
    } catch {
      setMessage("提交请求失败");
    }
  }

  async function handleSubmitSolution() {
    if (!solutionLink.trim()) {
      setMessage("解答链接不能为空");
      return;
    }
    try {
      const resp = await submitPushSolution(solvePushId, solutionLink.trim(), solutionDesc.trim() || null);
      if (resp.code === 200) {
        setSolvePushId(null);
        setSolutionLink("");
        setSolutionDesc("");
        setMessage("解答提交成功");
      } else {
        setMessage(resp.message || "提交失败");
      }
    } catch {
      setMessage("提交请求失败");
    }
  }

  async function handleApprove(pushId) {
    try {
      await approvePushItem(pushId);
      loadPushItems();
    } catch {
      setMessage("审批失败");
    }
  }

  async function handleReject(pushId) {
    try {
      await rejectPushItem(pushId);
      loadPushItems();
    } catch {
      setMessage("拒绝失败");
    }
  }

  async function handlePromote(pushId) {
    try {
      await promotePushItem(pushId);
      loadPushItems();
    } catch {
      setMessage("提升失败");
    }
  }

  async function loadSubmissions(pushId) {
    try {
      const resp = await getPushSubmissions(pushId);
      if (resp.code === 200) {
        setViewSubmissions({ pushId, items: resp.data || [] });
      }
    } catch {
      // ignore
    }
  }

  const approvedItems = pushItems.filter((i) => i.status === "APPROVED");
  const pendingItems = pushItems.filter((i) => i.status === "PENDING");
  const myItems = pushItems.filter((i) => i.status !== "APPROVED");

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">推题系统</h1>

      {message && (
        <p className="text-sm bg-bg-secondary text-text-secondary rounded-ui px-3 py-2 m-0">{message}</p>
      )}

      <Accordion.Root type="multiple" className="space-y-2">
        {/* Today's / Approved push items */}
        <Accordion.Item value="approved" className="bg-white border border-border rounded-ui overflow-hidden">
          <Accordion.Trigger className="flex items-center justify-between w-full px-4 py-3 text-sm font-medium text-text-primary hover:bg-bg-secondary border-0 bg-transparent cursor-pointer">
            <span>已发布推题 ({approvedItems.length})</span>
            <Chevron />
          </Accordion.Trigger>
          <Accordion.Content className="border-t border-border px-4 py-3">
            {loading ? (
              <ListSkeleton rows={3} />
            ) : approvedItems.length === 0 ? (
              <EmptyState title="暂无已发布推题" />
            ) : (
              <div className="space-y-2">
                {approvedItems.map((item) => (
                  <PushItemCard
                    key={item.id}
                    item={item}
                    isAdmin={isAdmin}
                    onSolve={() => setSolvePushId(item.id)}
                    onPromote={() => handlePromote(item.id)}
                    onViewSubmissions={() => loadSubmissions(item.id)}
                  />
                ))}
              </div>
            )}
          </Accordion.Content>
        </Accordion.Item>

        {/* Push history */}
        <Accordion.Item value="history" className="bg-white border border-border rounded-ui overflow-hidden">
          <Accordion.Trigger className="flex items-center justify-between w-full px-4 py-3 text-sm font-medium text-text-primary hover:bg-bg-secondary border-0 bg-transparent cursor-pointer">
            <span>推题历史 ({pushHistory.length})</span>
            <Chevron />
          </Accordion.Trigger>
          <Accordion.Content className="border-t border-border px-4 py-3">
            {pushHistory.length === 0 ? (
              <EmptyState title="暂无推题历史" />
            ) : (
              <div className="space-y-2">
                {pushHistory.map((item, idx) => (
                  <div key={item.id || idx} className="p-3 border border-border rounded-ui">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <h3 className="text-sm font-medium text-text-primary m-0 truncate">{item.title}</h3>
                        {item.link && (
                          <a href={item.link} target="_blank" rel="noreferrer" className="text-xs text-text-secondary underline">
                            打开链接
                          </a>
                        )}
                      </div>
                      <span className="text-xs text-text-secondary flex-shrink-0">{item.pushDate}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Accordion.Content>
        </Accordion.Item>

        {/* Pending review (admin) or my submissions (user) */}
        <Accordion.Item value="pending" className="bg-white border border-border rounded-ui overflow-hidden">
          <Accordion.Trigger className="flex items-center justify-between w-full px-4 py-3 text-sm font-medium text-text-primary hover:bg-bg-secondary border-0 bg-transparent cursor-pointer">
            <span>
              {isAdmin ? `待审核推题 (${pendingItems.length})` : `我的推题 (${myItems.length})`}
            </span>
            <Chevron />
          </Accordion.Trigger>
          <Accordion.Content className="border-t border-border px-4 py-3">
            {isAdmin ? (
              pendingItems.length === 0 ? (
                <EmptyState title="暂无待审核推题" />
              ) : (
                <div className="space-y-2">
                  {pendingItems.map((item) => (
                    <div key={item.id} className="flex items-start justify-between gap-3 p-3 border border-border rounded-ui">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-text-primary m-0 truncate">{item.title}</p>
                        {item.link && (
                          <a href={item.link} target="_blank" rel="noreferrer" className="text-xs text-text-secondary underline">
                            打开链接
                          </a>
                        )}
                      </div>
                      <div className="flex gap-1 flex-shrink-0">
                        <button
                          className="px-2 py-1 text-xs font-medium text-white bg-success hover:bg-[#268845] rounded border-0 cursor-pointer"
                          onClick={() => handleApprove(item.id)}
                        >
                          通过
                        </button>
                        <button
                          className="px-2 py-1 text-xs font-medium text-white bg-error hover:bg-[#b01e28] rounded border-0 cursor-pointer"
                          onClick={() => handleReject(item.id)}
                        >
                          拒绝
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )
            ) : myItems.length === 0 ? (
              <EmptyState title="暂无我的推题" description="提交推题后，管理员审核通过即可发布" />
            ) : (
              <div className="space-y-2">
                {myItems.map((item) => (
                  <div key={item.id} className="flex items-start justify-between gap-3 p-3 border border-border rounded-ui">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-text-primary m-0 truncate">{item.title}</p>
                      {item.link && (
                        <a href={item.link} target="_blank" rel="noreferrer" className="text-xs text-text-secondary underline">
                          打开链接
                        </a>
                      )}
                      <span className={`text-xs px-1.5 py-0.5 rounded mt-1 inline-block ${
                        item.status === "PENDING"
                          ? "bg-[#fff8f0] text-warning"
                          : "bg-[#fff0f0] text-error"
                      }`}>
                        {item.status === "PENDING" ? "审核中" : "已拒绝"}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Accordion.Content>
        </Accordion.Item>

        {/* Submit new push */}
        <Accordion.Item value="submit" className="bg-white border border-border rounded-ui overflow-hidden">
          <Accordion.Trigger className="flex items-center justify-between w-full px-4 py-3 text-sm font-medium text-text-primary hover:bg-bg-secondary border-0 bg-transparent cursor-pointer">
            <span>提交推题</span>
            <Chevron />
          </Accordion.Trigger>
          <Accordion.Content className="border-t border-border px-4 py-3">
            <div className="space-y-3">
              <input
                className="w-full px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="题目标题 *"
              />
              <input
                className="w-full px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                value={link}
                onChange={(e) => setLink(e.target.value)}
                placeholder="题目链接 *"
              />
              <textarea
                className="w-full px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary resize-y"
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="题目描述（可选）"
              />
              <button
                className="px-4 py-2 text-sm font-medium text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer"
                onClick={handleSubmitPush}
              >
                提交推题
              </button>
            </div>
          </Accordion.Content>
        </Accordion.Item>
      </Accordion.Root>

      {/* Solve modal (inline) */}
      {solvePushId && (
        <div className="fixed inset-0 bg-black/30 z-40 flex items-center justify-center">
          <div className="bg-white rounded-ui border border-border p-6 w-[400px] max-w-[90vw] shadow-lg z-50">
            <h3 className="text-base font-semibold text-text-primary m-0 mb-4">提交解答</h3>
            <input
              className="w-full px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary mb-2"
              value={solutionLink}
              onChange={(e) => setSolutionLink(e.target.value)}
              placeholder="解答链接 *"
            />
            <textarea
              className="w-full px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary resize-y mb-3"
              rows={2}
              value={solutionDesc}
              onChange={(e) => setSolutionDesc(e.target.value)}
              placeholder="解答描述（可选）"
            />
            <div className="flex justify-end gap-2">
              <button
                className="px-3 py-1.5 text-sm border border-border rounded-ui bg-white text-text-primary hover:bg-bg-secondary"
                onClick={() => setSolvePushId(null)}
              >
                取消
              </button>
              <button
                className="px-3 py-1.5 text-sm text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer"
                onClick={handleSubmitSolution}
              >
                提交
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function PushItemCard({ item, isAdmin, onSolve, onPromote, onViewSubmissions }) {
  return (
    <div className="p-3 border border-border rounded-ui">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-medium text-text-primary m-0 truncate">{item.title}</h3>
          {item.description ? (
            <p className="text-xs text-text-secondary mt-0.5 m-0 line-clamp-2">{item.description}</p>
          ) : (
            <p className="text-xs text-text-secondary mt-0.5 m-0">无描述</p>
          )}
        </div>
        <div className="flex gap-1 flex-shrink-0">
          <button
            className="px-2 py-1 text-xs font-medium text-white bg-success hover:bg-[#268845] rounded border-0 cursor-pointer"
            onClick={onSolve}
          >
            提交解答
          </button>
          {isAdmin && (
            <button
              className="px-2 py-1 text-xs font-medium text-text-secondary border border-border rounded bg-white hover:bg-bg-secondary cursor-pointer"
              onClick={onPromote}
            >
              提升
            </button>
          )}
          <button
            className="px-2 py-1 text-xs text-text-secondary border border-border rounded bg-white hover:bg-bg-secondary cursor-pointer"
            onClick={onViewSubmissions}
          >
            查看提交
          </button>
        </div>
      </div>
    </div>
  );
}

function Chevron() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}
