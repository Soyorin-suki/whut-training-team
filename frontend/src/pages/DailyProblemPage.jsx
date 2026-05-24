import { useEffect, useState, useCallback, useMemo } from "react";
import { getTodayProblem, checkInToday, getDailyHistory } from "../api/dailyProblem";
import ProblemCard from "../components/ui/ProblemCard";
import CheckInModal from "../components/ui/CheckInModal";
import Pagination from "../components/ui/Pagination";
import { CardSkeleton } from "../components/ui/Skeleton";
import EmptyState from "../components/ui/EmptyState";
import { useAuth } from "../context/AuthContext";

const PAGE_SIZE = 10;

function getTodayStr() {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Parse contestId and problemIndex from problemKey like "1234-A".
 */
function parseProblemKey(key) {
  const idx = key?.lastIndexOf("-");
  if (idx > 0) {
    return {
      contestId: Number(key.slice(0, idx)) || undefined,
      problemIndex: key.slice(idx + 1) || undefined,
    };
  }
  return { contestId: undefined, problemIndex: undefined };
}

export default function DailyProblemPage() {
  const { isAdmin } = useAuth();
  const [todayData, setTodayData] = useState(null);
  const [historyData, setHistoryData] = useState([]);
  const [historyPage, setHistoryPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Check-in modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [checkInLoading, setCheckInLoading] = useState(false);
  const [checkInResult, setCheckInResult] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [todayResp, historyResp] = await Promise.all([
        getTodayProblem(),
        getDailyHistory(0),
      ]);
      if (todayResp.code === 200) {
        setTodayData(todayResp.data);
      } else {
        setError(todayResp.message || "获取今日题失败");
      }
      if (historyResp.code === 200) {
        setHistoryData(historyResp.data || []);
      }
    } catch {
      setError("加载每日题数据失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  async function handleCheckIn(submissionId) {
    setCheckInLoading(true);
    try {
      const resp = await checkInToday(submissionId);
      if (resp.code === 200) {
        setCheckInResult(resp.data);
        loadData(); // refresh
      } else {
        setCheckInResult({
          accepted: false,
          submissionId,
          verdict: resp.message || "校验失败",
          score: 0,
        });
      }
    } catch {
      setCheckInResult({
        accepted: false,
        submissionId,
        verdict: "请求失败",
        score: 0,
      });
    } finally {
      setCheckInLoading(false);
    }
  }

  function openCheckIn(slot) {
    setSelectedSlot(slot);
    setCheckInResult(null);
    setModalOpen(true);
  }

  function handleModalClose(open) {
    if (!open) {
      setModalOpen(false);
      setCheckInResult(null);
      setSelectedSlot(null);
    }
  }

  // Compute today's display logic
  const today = getTodayStr();
  const todayDisplay = useMemo(() => {
    const todayHistory = historyData.filter((item) => item.date === today);
    // Redrawn items: checked-in → show in today area; not checked-in → hide from today
    const redrawnCheckedIn = todayHistory.filter((item) => item.isRedrawn && item.checkedIn);
    const redrawnNotCheckedInKeys = new Set(
      todayHistory.filter((item) => item.isRedrawn && !item.checkedIn).map((item) => item.problemKey)
    );
    // Active slots from API, excluding redrawn-not-checked-in ones
    const active = (todayData?.problems || []).filter(
      (p) => !redrawnNotCheckedInKeys.has(p.problemKey)
    );
    // Convert checked-in redrawn history items to display format
    const extra = redrawnCheckedIn.map((item) => {
      const pk = parseProblemKey(item.problemKey);
      return {
        ...item,
        type: (item.slot || "DAILY").toUpperCase(),
        contestId: pk.contestId,
        problemIndex: pk.problemIndex,
        tags: "",
        isRedrawnCheckedIn: true,
      };
    });
    return { active, extra };
  }, [todayData, historyData, today]);

  if (loading) {
    return (
      <div className="space-y-4">
        <h1 className="text-lg font-semibold">每日一题</h1>
        <CardSkeleton />
        <CardSkeleton />
      </div>
    );
  }

  // Group history by date for display
  const groupedHistory = groupByDate(historyData);
  const historyDates = Object.keys(groupedHistory).sort().reverse();
  const totalHistoryPages = Math.ceil(historyDates.length / PAGE_SIZE);
  const pagedDates = historyDates.slice(
    (historyPage - 1) * PAGE_SIZE,
    historyPage * PAGE_SIZE
  );

  const hasTodayContent =
    todayDisplay.active.length > 0 || todayDisplay.extra.length > 0;

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">每日一题</h1>

      {error && (
        <p className="text-sm text-error bg-[#fff0f0] rounded-ui px-3 py-2 m-0">{error}</p>
      )}

      {/* Today's problems */}
      {hasTodayContent ? (
        <div className="space-y-3">
          {/* Active (non-redrawn) slots */}
          {todayDisplay.active.map((p) => (
            <div key={p.problemKey}>
              <ProblemCard problem={p} />
              <div className="mt-1.5 flex justify-end">
                <button
                  className="px-3 py-1 text-xs font-medium text-white bg-success hover:bg-[#268845] rounded-ui border-0 cursor-pointer disabled:opacity-40"
                  disabled={todayData.checkedIn}
                  onClick={() => openCheckIn(p)}
                >
                  {todayData.checkedIn ? "今日已打卡" : "提交打卡"}
                </button>
              </div>
            </div>
          ))}
          {/* Redrawn but checked-in slots — show as 已打卡 */}
          {todayDisplay.extra.map((p) => (
            <div key={p.problemKey}>
              <ProblemCard problem={p} />
              <div className="mt-1.5 flex justify-end gap-2 items-center">
                <span className="text-xs text-text-secondary bg-bg-secondary px-2 py-0.5 rounded">
                  题目已刷新 · 已打卡
                </span>
                <span className="text-sm text-success font-medium">
                  +{p.score ?? 0}
                </span>
              </div>
            </div>
          ))}
          {todayData.checkedIn && (
            <p className="text-sm text-success m-0">
              今日已打卡，当前得分：{todayData.score ?? 0}
            </p>
          )}
        </div>
      ) : (
        <div className="bg-white border border-border rounded-ui p-8 text-center text-sm text-text-secondary">
          今日题目尚未生成
        </div>
      )}

      {/* History */}
      <section>
        <h2 className="text-base font-semibold text-text-primary m-0 mb-3">历史记录</h2>
        {historyDates.length === 0 ? (
          <EmptyState title="暂无历史记录" />
        ) : (
          <div className="space-y-1">
            {pagedDates.map((date) => {
              const items = groupedHistory[date];
              return (
                <details key={date} className="bg-white border border-border rounded-ui group">
                  <summary className="flex items-center justify-between px-4 py-2.5 cursor-pointer hover:bg-bg-secondary list-none">
                    <span className="text-sm font-medium text-text-primary">{date}</span>
                    <span className="text-xs text-text-secondary">
                      {items.filter((i) => i.checkedIn).length}/{items.length} 已打卡
                    </span>
                  </summary>
                  <div className="border-t border-border">
                    {items.map((item, idx) => (
                      <div
                        key={idx}
                        className="flex items-center justify-between px-4 py-2 text-sm"
                      >
                        <div className="flex items-center gap-2">
                          <span
                            className={`text-xs px-1.5 py-0.5 rounded ${
                              item.slot === "easy"
                                ? "bg-[#f0fff0] text-success"
                                : "bg-[#fff8f0] text-warning"
                            }`}
                          >
                            {item.slot ?? "?"}
                          </span>
                          <a
                            href={item.sourceUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-text-primary hover:underline"
                          >
                            {item.name}
                          </a>
                          <span className="text-text-secondary">({item.rating ?? "-"})</span>
                          {item.isRedrawn && !item.checkedIn && (
                            <span className="text-[11px] text-text-secondary bg-bg-secondary px-1.5 py-0.5 rounded">
                              已被刷新
                            </span>
                          )}
                          {item.isRedrawn && item.checkedIn && (
                            <span className="text-[11px] text-text-secondary">已打卡（已刷新）</span>
                          )}
                        </div>
                        <div>
                          {item.checkedIn ? (
                            <span className="text-success font-medium">
                              +{item.score ?? 0}
                            </span>
                          ) : (
                            <span className="text-text-secondary">未打卡</span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </details>
              );
            })}
          </div>
        )}
        <Pagination
          page={historyPage}
          totalPages={totalHistoryPages}
          onPageChange={setHistoryPage}
        />
      </section>

      {/* Check-in modal */}
      <CheckInModal
        open={modalOpen}
        onOpenChange={handleModalClose}
        onCheckIn={handleCheckIn}
        loading={checkInLoading}
        result={checkInResult}
        title={selectedSlot ? `打卡 ${selectedSlot.name}` : "提交打卡"}
      />
    </div>
  );
}

function groupByDate(historyItems) {
  const groups = {};
  for (const item of historyItems) {
    const d = item.date || "unknown";
    if (!groups[d]) groups[d] = [];
    groups[d].push(item);
  }
  return groups;
}
