import { useCallback, useEffect, useMemo, useState } from "react";
import { checkInToday, getDailyHistory, getTodayProblem } from "../api/dailyProblem";
import CheckInModal from "../components/ui/CheckInModal";
import EmptyState from "../components/ui/EmptyState";
import Pagination from "../components/ui/Pagination";
import ProblemCard from "../components/ui/ProblemCard";
import { CardSkeleton } from "../components/ui/Skeleton";

const PAGE_SIZE = 10;
const TEXT = {
  title: "\u6bcf\u65e5\u4e00\u9898",
  loadTodayFailed: "\u83b7\u53d6\u4eca\u65e5\u9898\u5931\u8d25",
  loadDataFailed: "\u52a0\u8f7d\u6bcf\u65e5\u9898\u6570\u636e\u5931\u8d25",
  checkInFailed: "\u6821\u9a8c\u5931\u8d25",
  requestFailed: "\u8bf7\u6c42\u5931\u8d25",
  checkedIn: "\u5df2\u6253\u5361",
  submitCheckIn: "\u63d0\u4ea4\u6253\u5361",
  redrawnCheckedIn: "\u9898\u76ee\u5df2\u5237\u65b0 \u00b7 \u5df2\u6253\u5361",
  notGenerated: "\u4eca\u65e5\u9898\u76ee\u5c1a\u672a\u751f\u6210",
  history: "\u5386\u53f2\u8bb0\u5f55",
  noHistory: "\u6682\u65e0\u5386\u53f2\u8bb0\u5f55",
  checkedCount: "\u5df2\u6253\u5361",
  redrawn: "\u5df2\u88ab\u5237\u65b0",
  checkedBeforeRedraw: "\u5df2\u6253\u5361\uff08\u5df2\u5237\u65b0\uff09",
  notCheckedIn: "\u672a\u6253\u5361",
  currentScore: "\u5f53\u524d\u5f97\u5206",
  completed: "\u4eca\u65e5\u5df2\u6253\u5361",
  problemsUnit: "\u9898",
  checkIn: "\u6253\u5361",
};

function getTodayStr() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

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
  const [todayData, setTodayData] = useState(null);
  const [historyData, setHistoryData] = useState([]);
  const [historyPage, setHistoryPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [checkInLoading, setCheckInLoading] = useState(false);
  const [checkInResult, setCheckInResult] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const todayResp = await getTodayProblem();
      const historyResp = await getDailyHistory(0);

      if (todayResp.code === 200) {
        setTodayData(todayResp.data);
      } else {
        setError(todayResp.message || TEXT.loadTodayFailed);
      }

      if (historyResp.code === 200) {
        setHistoryData(historyResp.data || []);
      }
    } catch {
      setError(TEXT.loadDataFailed);
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
        loadData();
      } else {
        setCheckInResult({
          accepted: false,
          submissionId,
          verdict: resp.message || TEXT.checkInFailed,
          score: 0,
        });
      }
    } catch {
      setCheckInResult({
        accepted: false,
        submissionId,
        verdict: TEXT.requestFailed,
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

  const today = getTodayStr();
  const todayDisplay = useMemo(() => {
    const todayHistory = historyData.filter((item) => item.date === today);
    const redrawnCheckedIn = todayHistory.filter((item) => item.isRedrawn && item.checkedIn);
    const redrawnNotCheckedInKeys = new Set(
      todayHistory
        .filter((item) => item.isRedrawn && !item.checkedIn)
        .map((item) => item.problemKey)
    );
    const active = (todayData?.problems || []).filter(
      (problem) => !redrawnNotCheckedInKeys.has(problem.problemKey)
    );
    const extra = redrawnCheckedIn.map((item) => {
      const parsed = parseProblemKey(item.problemKey);
      return {
        ...item,
        type: (item.slot || "DAILY").toUpperCase(),
        contestId: parsed.contestId,
        problemIndex: parsed.problemIndex,
        tags: "",
        isRedrawnCheckedIn: true,
      };
    });
    return { active, extra };
  }, [todayData, historyData, today]);

  if (loading) {
    return (
      <div className="space-y-4">
        <h1 className="text-lg font-semibold">{TEXT.title}</h1>
        <CardSkeleton />
        <CardSkeleton />
      </div>
    );
  }

  const groupedHistory = groupByDate(historyData);
  const historyDates = Object.keys(groupedHistory).sort().reverse();
  const totalHistoryPages = Math.ceil(historyDates.length / PAGE_SIZE);
  const pagedDates = historyDates.slice(
    (historyPage - 1) * PAGE_SIZE,
    historyPage * PAGE_SIZE
  );

  const hasTodayContent =
    todayDisplay.active.length > 0 || todayDisplay.extra.length > 0;
  const todayCompletedCount =
    todayDisplay.active.filter((problem) => problem.checkedIn).length + todayDisplay.extra.length;
  const todayTotalCount = todayDisplay.active.length + todayDisplay.extra.length;

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">{TEXT.title}</h1>

      {error && (
        <p className="text-sm text-error bg-[#fff0f0] rounded-ui px-3 py-2 m-0">{error}</p>
      )}

      {hasTodayContent ? (
        <div className="space-y-3">
          {todayDisplay.active.map((problem) => (
            <div key={problem.problemKey}>
              <ProblemCard problem={problem} />
              <div className="mt-1.5 flex justify-end">
                <button
                  className="px-3 py-1 text-xs font-medium text-white bg-success hover:bg-[#268845] rounded-ui border-0 cursor-pointer disabled:opacity-40"
                  disabled={problem.checkedIn}
                  onClick={() => openCheckIn(problem)}
                >
                  {problem.checkedIn ? TEXT.checkedIn : TEXT.submitCheckIn}
                </button>
              </div>
            </div>
          ))}

          {todayDisplay.extra.map((problem) => (
            <div key={problem.problemKey}>
              <ProblemCard problem={problem} />
              <div className="mt-1.5 flex justify-end gap-2 items-center">
                <span className="text-xs text-text-secondary bg-bg-secondary px-2 py-0.5 rounded">
                  {TEXT.redrawnCheckedIn}
                </span>
                <span className="text-sm text-success font-medium">
                  +{problem.score ?? 0}
                </span>
              </div>
            </div>
          ))}

          {todayData?.checkedIn && (
            <p className="text-sm text-success m-0">
              {TEXT.completed} {todayCompletedCount}/{todayTotalCount} {TEXT.problemsUnit}
              {", "}
              {TEXT.currentScore}: {todayData.score ?? 0}
            </p>
          )}
        </div>
      ) : (
        <div className="bg-white border border-border rounded-ui p-8 text-center text-sm text-text-secondary">
          {TEXT.notGenerated}
        </div>
      )}

      <section>
        <h2 className="text-base font-semibold text-text-primary m-0 mb-3">{TEXT.history}</h2>
        {historyDates.length === 0 ? (
          <EmptyState title={TEXT.noHistory} />
        ) : (
          <div className="space-y-1">
            {pagedDates.map((date) => {
              const items = groupedHistory[date];
              return (
                <details key={date} className="bg-white border border-border rounded-ui group">
                  <summary className="flex items-center justify-between px-4 py-2.5 cursor-pointer hover:bg-bg-secondary list-none">
                    <span className="text-sm font-medium text-text-primary">{date}</span>
                    <span className="text-xs text-text-secondary">
                      {items.filter((item) => item.checkedIn).length}/{items.length} {TEXT.checkedCount}
                    </span>
                  </summary>
                  <div className="border-t border-border">
                    {items.map((item, index) => (
                      <div
                        key={`${item.problemKey}-${index}`}
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
                              {TEXT.redrawn}
                            </span>
                          )}
                          {item.isRedrawn && item.checkedIn && (
                            <span className="text-[11px] text-text-secondary">{TEXT.checkedBeforeRedraw}</span>
                          )}
                        </div>
                        <div>
                          {item.checkedIn ? (
                            <span className="text-success font-medium">
                              +{item.score ?? 0}
                            </span>
                          ) : (
                            <span className="text-text-secondary">{TEXT.notCheckedIn}</span>
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

      <CheckInModal
        open={modalOpen}
        onOpenChange={handleModalClose}
        onCheckIn={handleCheckIn}
        loading={checkInLoading}
        result={checkInResult}
        title={selectedSlot ? `${TEXT.checkIn} ${selectedSlot.name}` : TEXT.submitCheckIn}
      />
    </div>
  );
}

function groupByDate(historyItems) {
  const groups = {};
  for (const item of historyItems) {
    const date = item.date || "unknown";
    if (!groups[date]) groups[date] = [];
    groups[date].push(item);
  }
  return groups;
}
