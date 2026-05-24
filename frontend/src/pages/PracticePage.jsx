import { useEffect, useState, useCallback } from "react";
import { drawPracticeProblem, checkPractice, getPracticeHistory, deletePracticeDraw } from "../api/dailyProblem";
import ProblemCard from "../components/ui/ProblemCard";
import EmptyState from "../components/ui/EmptyState";
import { ListSkeleton } from "../components/ui/Skeleton";

export default function PracticePage() {
  const [minRating, setMinRating] = useState(1200);
  const [maxRating, setMaxRating] = useState(1600);
  const [drawResult, setDrawResult] = useState(null);
  const [submissionId, setSubmissionId] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [checkResult, setCheckResult] = useState(null);

  // Practice history
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [deletingId, setDeletingId] = useState(null);

  const loadHistory = useCallback(async () => {
    try {
      const resp = await getPracticeHistory(50);
      if (resp.code === 200) {
        setHistory(resp.data || []);
      }
    } catch {
      // ignore
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  useEffect(() => { loadHistory(); }, [loadHistory]);

  async function handleDraw() {
    setLoading(true);
    setMessage("");
    setDrawResult(null);
    setCheckResult(null);
    try {
      const resp = await drawPracticeProblem(
        minRating || null,
        maxRating || null
      );
      if (resp.code === 200) {
        setDrawResult(resp.data);
        loadHistory(); // refresh history after draw
      } else {
        setMessage(resp.message || "抽题失败");
      }
    } catch {
      setMessage("抽题请求失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleCheck() {
    if (!drawResult?.drawId) {
      setMessage("请先抽题");
      return;
    }
    if (!submissionId.trim()) {
      setMessage("请输入提交 ID");
      return;
    }
    setLoading(true);
    setMessage("");
    try {
      const resp = await checkPractice(drawResult.drawId, Number(submissionId));
      if (resp.code === 200) {
        setCheckResult(resp.data);
        if (resp.data?.accepted) {
          setMessage("练习题通过（不计分）");
        } else {
          setMessage(`练习题未通过，verdict=${resp.data?.verdict || "-"}`);
        }
        loadHistory(); // refresh history after check
      } else {
        setMessage(resp.message || "校验失败");
      }
    } catch {
      setMessage("校验请求失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(drawId) {
    setDeletingId(drawId);
    try {
      const resp = await deletePracticeDraw(drawId);
      if (resp.code === 200) {
        setHistory((prev) => prev.filter((item) => item.drawId !== drawId));
      }
    } catch {
      setMessage("删除失败");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">自主练习</h1>
      <p className="text-sm text-text-secondary m-0">按 rating 范围随机抽题练习，不计分</p>

      {/* Draw panel */}
      <div className="bg-white border border-border rounded-ui p-4">
        <div className="flex items-end gap-3 flex-wrap">
          <label className="flex flex-col gap-1">
            <span className="text-xs text-text-secondary">最小难度</span>
            <input
              className="w-24 px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
              type="number"
              value={minRating}
              onChange={(e) => setMinRating(e.target.value)}
            />
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-text-secondary">最大难度</span>
            <input
              className="w-24 px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
              type="number"
              value={maxRating}
              onChange={(e) => setMaxRating(e.target.value)}
            />
          </label>
          <button
            className="px-4 py-2 text-sm font-medium text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer disabled:opacity-50"
            onClick={handleDraw}
            disabled={loading}
          >
            {loading ? "抽取中..." : "抽题"}
          </button>
        </div>
      </div>

      {message && (
        <p
          className={`text-sm rounded-ui px-3 py-2 m-0 ${
            message.includes("通过") || message.includes("成功")
              ? "bg-[#f0fff0] text-success"
              : "text-text-secondary bg-bg-secondary"
          }`}
        >
          {message}
        </p>
      )}

      {/* Draw result */}
      {drawResult?.problem && <ProblemCard problem={drawResult.problem} />}

      {/* Check submission */}
      {drawResult && (
        <div className="bg-white border border-border rounded-ui p-4">
          <div className="flex items-end gap-3 flex-wrap">
            <label className="flex flex-col gap-1 flex-1 min-w-[200px]">
              <span className="text-xs text-text-secondary">提交校验（不计分）</span>
              <input
                className="px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                value={submissionId}
                onChange={(e) => setSubmissionId(e.target.value)}
                placeholder="输入 Codeforces 提交 ID"
              />
            </label>
            <button
              className="px-4 py-2 text-sm font-medium text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer disabled:opacity-50"
              onClick={handleCheck}
              disabled={loading}
            >
              提交校验
            </button>
          </div>
          {checkResult && (
            <p
              className={`text-sm mt-2 m-0 ${
                checkResult.accepted ? "text-success" : "text-error"
              }`}
            >
              {checkResult.accepted ? "通过" : "未通过"} · verdict: {checkResult.verdict ?? "-"}
            </p>
          )}
        </div>
      )}

      {/* Practice history */}
      <section>
        <h2 className="text-base font-semibold text-text-primary m-0 mb-2">抽题历史</h2>
        {historyLoading ? (
          <ListSkeleton rows={3} />
        ) : history.length === 0 ? (
          <EmptyState title="暂无抽题记录" description="点击上方按钮开始抽题练习" />
        ) : (
          <div className="space-y-2">
            {history.map((item) => (
              <div
                key={item.drawId}
                className="bg-white border border-border rounded-ui p-3 flex items-center justify-between gap-3"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <a
                      href={item.sourceUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-sm font-medium text-text-primary hover:underline truncate"
                    >
                      {item.name}
                    </a>
                    <span className="text-xs text-text-secondary">
                      ({item.rating ?? "-"})
                    </span>
                    {item.verdict && (
                      <span
                        className={`text-xs px-1.5 py-0.5 rounded ${
                          item.verdict === "OK"
                            ? "bg-[#f0fff0] text-success"
                            : "bg-[#fff0f0] text-error"
                        }`}
                      >
                        {item.verdict}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-text-secondary mt-0.5 m-0">
                    {item.drawDate}{item.drawnAt ? ` · ${item.drawnAt.slice(11, 19)}` : ""}
                  </p>
                </div>
                <button
                  className="flex-shrink-0 px-2 py-1 text-xs text-error border border-border rounded hover:bg-[#fff0f0] cursor-pointer disabled:opacity-40 bg-white"
                  onClick={() => handleDelete(item.drawId)}
                  disabled={deletingId === item.drawId}
                >
                  {deletingId === item.drawId ? "..." : "删除"}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
