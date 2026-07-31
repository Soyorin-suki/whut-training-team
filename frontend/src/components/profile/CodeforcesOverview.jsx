import { useEffect, useMemo, useState } from "react";
import { ExternalLink, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import {
  getCodeforcesOverview,
  refreshCodeforcesOverview,
} from "../../api/user";
import { getRatingMeta } from "../../utils/cf";
import { CardSkeleton } from "../ui/Skeleton";

const CHART_COLORS = [
  "#111111",
  "#353535",
  "#585858",
  "#7d7d7d",
  "#a1a1a1",
  "#bdbdbd",
  "#d5d5d5",
  "#e8e8e8",
];

export default function CodeforcesOverview({
  userId,
  handle,
  canRefresh = false,
  compact = false,
}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(Boolean(userId && handle));
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!userId || !handle) {
      setData(null);
      setLoading(false);
      return;
    }
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const response = await getCodeforcesOverview(userId);
        if (!cancelled) {
          if (response.code === 200) setData(response.data);
          else setError(response.message || "Codeforces 数据加载失败");
        }
      } catch {
        if (!cancelled) setError("Codeforces 数据暂时不可用");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [userId, handle]);

  async function handleRefresh() {
    setRefreshing(true);
    setError("");
    try {
      const response = await refreshCodeforcesOverview(userId);
      if (response.code === 200) setData(response.data);
      else setError(response.message || "刷新失败");
    } catch {
      setError("刷新失败，请稍后重试");
    } finally {
      setRefreshing(false);
    }
  }

  if (!handle) {
    return compact ? null : (
      <section className="bg-white border border-border rounded-ui p-5">
        <h2 className="text-base font-semibold text-text-primary m-0">Codeforces 数据</h2>
        <p className="text-sm text-text-secondary mt-2 mb-0">
          绑定 Codeforces 账号后，这里会展示做题数、标签分布和近期 rated 比赛。
        </p>
      </section>
    );
  }

  if (loading) {
    return <CardSkeleton />;
  }

  if (error && !data) {
    return (
      <section className="bg-white border border-border rounded-ui p-5">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-semibold text-text-primary m-0">
              Codeforces 数据
            </h2>
            <p className="text-sm text-error mt-1 mb-0">{error}</p>
          </div>
          {canRefresh && (
            <button className="button-primary inline-flex items-center gap-1.5" onClick={handleRefresh}>
              重试
            </button>
          )}
        </div>
      </section>
    );
  }

  if (compact) {
    return (
      <section className="bg-white border border-border rounded-ui p-4">
        <div className="flex items-center justify-between gap-3 mb-3">
          <div>
            <p className="dashboard-eyebrow m-0">CODEFORCES OVERVIEW</p>
            <a
              href={`https://codeforces.com/profile/${encodeURIComponent(handle)}`}
              target="_blank"
              rel="noreferrer"
              className="text-sm font-semibold text-text-primary no-underline hover:underline"
            >
              @{handle}
            </a>
          </div>
          <Link to="/profile" className="text-xs text-text-secondary hover:text-text-primary">
            查看完整数据 →
          </Link>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <CompactStat label="已解决" value={data?.solvedCount ?? 0} />
          <CompactStat label="当前 Rating" value={data?.currentRating ?? "-"} />
          <CompactStat label="Rated 比赛" value={data?.ratedContestCount ?? 0} />
        </div>
      </section>
    );
  }

  const ratingMeta = getRatingMeta(data?.currentRating);
  return (
    <section className="space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-text-primary m-0">
            Codeforces 数据
          </h2>
          <div className="flex flex-wrap items-center gap-2 mt-1">
            <a
              href={`https://codeforces.com/profile/${encodeURIComponent(handle)}`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-xs text-text-secondary hover:text-text-primary"
            >
              @{handle} <ExternalLink size={12} />
            </a>
            {data?.stale && (
              <span className="text-[11px] text-text-secondary">
                正在后台更新，当前显示上次快照
              </span>
            )}
          </div>
        </div>
        {canRefresh && (
          <button
            className="button-primary inline-flex items-center gap-1.5"
            onClick={handleRefresh}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? "animate-spin" : ""} />
            {refreshing ? "同步中..." : "同步 CF"}
          </button>
        )}
      </div>

      {error && <p className="text-xs text-error m-0">{error}</p>}

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <OverviewStat label="已解决题目" value={data?.solvedCount ?? 0} />
        <OverviewStat label="尝试过题目" value={data?.attemptedCount ?? 0} />
        <OverviewStat
          label="当前 Rating"
          value={data?.currentRating ?? "-"}
          valueColor={ratingMeta.color}
          hint={`最高 ${data?.maxRating ?? "-"}`}
        />
        <OverviewStat label="Rated 比赛" value={data?.ratedContestCount ?? 0} />
      </div>

      {data?.submissionLimitReached && (
        <p className="text-[11px] text-text-secondary m-0">
          该账号提交超过 10,000 条，做题统计基于最近 10,000 条提交。
        </p>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.4fr)] gap-3">
        <TagDonutChart items={data?.tagStats || []} />
        <RecentContests items={data?.recentContests || []} />
      </div>

      {data?.syncedAt && (
        <p className="text-[11px] text-text-secondary text-right m-0">
          数据同步于 {new Date(data.syncedAt).toLocaleString("zh-CN")}
        </p>
      )}
    </section>
  );
}

function OverviewStat({ label, value, hint, valueColor }) {
  return (
    <div className="bg-white border border-border rounded-ui p-4">
      <p className="text-xs font-semibold tracking-wide text-text-secondary m-0">
        {label}
      </p>
      <strong
        className="block text-2xl leading-none mt-3 text-text-primary"
        style={valueColor ? { color: valueColor } : undefined}
      >
        {value}
      </strong>
      {hint && <small className="block text-xs text-text-secondary mt-1">{hint}</small>}
    </div>
  );
}

function CompactStat({ label, value }) {
  return (
    <div className="bg-bg-secondary rounded-ui px-3 py-2.5">
      <span className="block text-[11px] font-semibold text-text-secondary">{label}</span>
      <strong className="block text-lg text-text-primary mt-0.5">{value}</strong>
    </div>
  );
}

function TagDonutChart({ items }) {
  const chartItems = useMemo(() => {
    const top = items.slice(0, 7);
    const otherCount = items.slice(7).reduce((sum, item) => sum + item.count, 0);
    return otherCount > 0 ? [...top, { tag: "其他", count: otherCount }] : top;
  }, [items]);
  const total = chartItems.reduce((sum, item) => sum + item.count, 0);
  let cursor = 0;
  const gradient = chartItems.map((item, index) => {
    const start = cursor;
    cursor += total ? (item.count / total) * 100 : 0;
    return `${CHART_COLORS[index]} ${start}% ${cursor}%`;
  }).join(", ");

  return (
    <div className="bg-white border border-border rounded-ui p-4">
      <h3 className="text-sm font-semibold text-text-primary mt-0 mb-4">题目标签分布</h3>
      {total === 0 ? (
        <p className="text-sm text-text-secondary my-12 text-center">暂无 AC 题目标签</p>
      ) : (
        <div className="flex flex-col sm:flex-row items-center gap-5">
          <div
            className="relative w-36 h-36 rounded-full flex-shrink-0"
            style={{ background: `conic-gradient(${gradient})` }}
            aria-label="题目标签饼状图"
          >
            <div className="absolute inset-[27%] rounded-full bg-white flex items-center justify-center text-center">
              <span>
                <strong className="block text-xl leading-none">{total}</strong>
                <small className="text-[10px] text-text-secondary">标签计数</small>
              </span>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-x-4 gap-y-2 w-full min-w-0">
            {chartItems.map((item, index) => (
              <div key={item.tag} className="flex items-center gap-2 min-w-0">
                <span
                  className="w-2.5 h-2.5 rounded-sm flex-shrink-0"
                  style={{ backgroundColor: CHART_COLORS[index] }}
                />
                <span className="text-xs text-text-secondary truncate flex-1">
                  {item.tag}
                </span>
                <strong className="text-xs text-text-primary">{item.count}</strong>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function RecentContests({ items }) {
  return (
    <div className="bg-white border border-border rounded-ui overflow-hidden">
      <h3 className="text-sm font-semibold text-text-primary m-0 px-4 py-3 border-b border-border">
        近期 Rated 比赛
      </h3>
      {items.length === 0 ? (
        <p className="text-sm text-text-secondary my-12 text-center">暂无 rated 比赛记录</p>
      ) : (
        <div>
          {items.map((contest, index) => (
            <a
              key={`${contest.contestId}-${contest.ratingUpdateTimeSeconds}`}
              href={contest.url}
              target="_blank"
              rel="noreferrer"
              className={`grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-3 px-4 py-3 no-underline hover:bg-bg-secondary ${
                index ? "border-t border-border" : ""
              }`}
            >
              <span className="min-w-0">
                <strong className="block text-sm text-text-primary truncate">
                  {contest.contestName || `Contest ${contest.contestId}`}
                </strong>
                <small className="text-[11px] text-text-secondary">
                  {formatContestDate(contest.ratingUpdateTimeSeconds)} · Rank {contest.rank ?? "-"}
                </small>
              </span>
              <span className="text-xs text-text-secondary">
                {contest.oldRating ?? "-"} → {contest.newRating ?? "-"}
              </span>
              <strong
                className={`text-sm min-w-10 text-right ${
                  (contest.ratingChange ?? 0) > 0
                    ? "text-success"
                    : (contest.ratingChange ?? 0) < 0
                      ? "text-error"
                      : "text-text-secondary"
                }`}
              >
                {(contest.ratingChange ?? 0) > 0 ? "+" : ""}
                {contest.ratingChange ?? 0}
              </strong>
            </a>
          ))}
        </div>
      )}
    </div>
  );
}

function formatContestDate(seconds) {
  if (!seconds) return "时间未知";
  return new Date(seconds * 1000).toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}
