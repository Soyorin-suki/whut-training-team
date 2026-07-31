import { useCallback, useEffect, useMemo, useState } from "react";
import { CalendarClock, Clock3, ExternalLink, Radio, RefreshCw } from "lucide-react";
import { getUpcomingContests } from "../api/contests";
import EmptyState from "../components/ui/EmptyState";
import { ListSkeleton } from "../components/ui/Skeleton";

const SHANGHAI_TIME = new Intl.DateTimeFormat("zh-CN", {
  timeZone: "Asia/Shanghai",
  month: "2-digit",
  day: "2-digit",
  weekday: "short",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

export default function ContestsPage() {
  const [contests, setContests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [now, setNow] = useState(() => Date.now());

  const loadContests = useCallback(async (manual = false) => {
    if (manual) setRefreshing(true);
    else setLoading(true);
    setError("");
    try {
      const resp = await getUpcomingContests();
      if (resp.code === 200) {
        setContests(resp.data || []);
      } else {
        setError(resp.message || "近期比赛加载失败");
      }
    } catch (requestError) {
      setError(requestError.response?.data?.message || "AtCoder 近期比赛暂时无法获取");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadContests();
  }, [loadContests]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const sortedContests = useMemo(
    () => [...contests].sort((a, b) => new Date(a.startTime) - new Date(b.startTime)),
    [contests]
  );

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3">
        <div>
          <p className="text-[11px] font-bold tracking-[0.18em] text-text-secondary m-0">
            CONTEST RADAR
          </p>
          <h1 className="text-xl font-bold text-text-primary mt-1 mb-0">近期比赛</h1>
          <p className="text-sm text-text-secondary mt-1 mb-0">
            AtCoder 官方赛程，时间已转换为北京时间。
          </p>
        </div>
        <button
          type="button"
          className="inline-flex items-center justify-center gap-2 px-3 py-2 text-sm border border-border rounded-ui bg-white text-text-primary hover:bg-bg-secondary disabled:opacity-50"
          onClick={() => loadContests(true)}
          disabled={refreshing}
        >
          <RefreshCw size={15} className={refreshing ? "animate-spin" : ""} />
          {refreshing ? "刷新中" : "刷新赛程"}
        </button>
      </div>

      {error && (
        <p className="text-sm text-error bg-[#fff0f0] rounded-ui px-3 py-2 m-0">{error}</p>
      )}

      {loading ? (
        <ListSkeleton rows={6} />
      ) : sortedContests.length === 0 ? (
        <EmptyState title="暂时没有公布的近期比赛" />
      ) : (
        <div className="grid gap-3">
          {sortedContests.map((contest, index) => {
            const status = getContestStatus(contest, now);
            return (
              <article
                key={`${contest.platform}-${contest.contestId}`}
                className="bg-white border border-border rounded-ui p-4 sm:p-5"
              >
                <div className="flex items-start gap-4">
                  <div className="hidden sm:flex w-12 h-12 flex-shrink-0 items-center justify-center rounded-full bg-[#f1f1ec] text-text-primary">
                    {status.live ? <Radio size={20} /> : <CalendarClock size={20} />}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-[10px] font-extrabold tracking-[0.14em] text-text-secondary">
                        ATCODER · {contest.type?.toUpperCase() || "CONTEST"}
                      </span>
                      <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                        status.live
                          ? "bg-[#e8f8eb] text-[#216e39]"
                          : index === 0
                            ? "bg-[#fff4df] text-[#8a5a00]"
                            : "bg-bg-secondary text-text-secondary"
                      }`}>
                        {status.label}
                      </span>
                    </div>
                    <h2 className="text-base sm:text-lg font-bold text-text-primary mt-2 mb-0">
                      {contest.name}
                    </h2>
                    <div className="flex flex-wrap gap-x-5 gap-y-1 mt-3 text-xs text-text-secondary">
                      <span className="inline-flex items-center gap-1.5">
                        <CalendarClock size={14} />
                        {SHANGHAI_TIME.format(new Date(contest.startTime))}
                      </span>
                      <span className="inline-flex items-center gap-1.5">
                        <Clock3 size={14} />
                        {formatDuration(contest.durationMinutes)}
                      </span>
                      <span>Rated: {contest.ratedRange || "-"}</span>
                    </div>
                  </div>
                  <a
                    className="inline-flex flex-shrink-0 items-center gap-1 text-xs font-semibold text-text-primary hover:underline"
                    href={contest.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    前往比赛
                    <ExternalLink size={14} />
                  </a>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <p className="text-xs text-text-secondary m-0">
        数据来自 AtCoder 官方 Upcoming Contests 页面，后端会短时缓存；具体安排以官方页面为准。
      </p>
    </div>
  );
}

function getContestStatus(contest, now) {
  const start = new Date(contest.startTime).getTime();
  const end = start + Math.max(0, contest.durationMinutes || 0) * 60_000;
  if (now >= start && now < end) {
    return { live: true, label: "正在进行" };
  }

  const diffMinutes = Math.max(0, Math.floor((start - now) / 60_000));
  if (diffMinutes < 60) {
    return { live: false, label: `${Math.max(1, diffMinutes)} 分钟后` };
  }
  if (diffMinutes < 24 * 60) {
    return { live: false, label: `${Math.floor(diffMinutes / 60)} 小时后` };
  }
  return { live: false, label: `${Math.floor(diffMinutes / (24 * 60))} 天后` };
}

function formatDuration(minutes) {
  if (!minutes) return "时长待定";
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  if (remaining === 0) return `${hours} 小时`;
  return `${hours} 小时 ${remaining} 分钟`;
}
