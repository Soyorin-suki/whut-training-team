import { useEffect, useState } from "react";
import { getUserInitial } from "../auth";
import { getLeaderboard, getMyLeaderboardRank } from "../api/rankings";

const LEADERBOARD_TYPES = [
  {
    key: "DAILY_TOTAL",
    label: "积分榜",
    title: "训练积分榜",
    description: "按每日训练累计积分排序，查看当前综合排名。",
    metricLabel: "积分",
    metricUnit: ""
  },
  {
    key: "SOLVED_COUNT",
    label: "做题数榜",
    title: "Codeforces 做题数榜",
    description: "按已通过题目数量排序，关注稳定输出能力。",
    metricLabel: "做题数",
    metricUnit: ""
  },
  {
    key: "HARD_SOLVED_COUNT",
    label: "难题数榜",
    title: "Codeforces 难题数榜",
    description: "按高难题通过数量排序，观察高分题攻坚表现。",
    metricLabel: "难题数",
    metricUnit: ""
  },
  {
    key: "CURRENT_STREAK",
    label: "当前连续打卡榜",
    title: "当前连续打卡榜",
    description: "按当前连续打卡天数排序，查看谁还在持续连击。",
    metricLabel: "连续天数",
    metricUnit: "天"
  },
  {
    key: "LONGEST_STREAK",
    label: "历史最长打卡榜",
    title: "历史最长打卡榜",
    description: "按历史最长连续打卡天数排序，查看谁的纪录更高。",
    metricLabel: "最长天数",
    metricUnit: "天"
  }
];

function createTabState() {
  return {
    page: 1,
    total: 0,
    entries: [],
    currentUserEntry: null,
    loading: false,
    message: "",
    hasLoaded: false
  };
}

function createLeaderboardStateByType() {
  return LEADERBOARD_TYPES.reduce((result, item) => {
    result[item.key] = createTabState();
    return result;
  }, {});
}

function formatMetric(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function formatMetricWithUnit(value, unit) {
  const formatted = formatMetric(value);
  return unit ? `${formatted} ${unit}` : formatted;
}

function getLeaderboardMeta(type) {
  return LEADERBOARD_TYPES.find((item) => item.key === type) || LEADERBOARD_TYPES[0];
}

function getFallbackMetric(type, user) {
  if (type === "DAILY_TOTAL") {
    return user?.score ?? 0;
  }
  if (type === "CURRENT_STREAK") {
    return user?.currentStreakDays ?? 0;
  }
  if (type === "LONGEST_STREAK") {
    return user?.longestStreakDays ?? 0;
  }
  return 0;
}

function LeaderboardRow({ entry, metricUnit }) {
  if (!entry) {
    return null;
  }

  return (
    <article className={`leaderboard-row ${entry.isCurrentUser ? "is-current-user" : ""}`}>
      <div className="leaderboard-rank">{entry.rank ?? "-"}</div>
      <div className="leaderboard-user">
        {entry.avatarUrl ? (
          <img className="leaderboard-avatar" src={entry.avatarUrl} alt="avatar" />
        ) : (
          <span className="leaderboard-avatar leaderboard-avatar-fallback">
            {getUserInitial({ username: entry.username })}
          </span>
        )}
        <div>
          <strong>{entry.username || "-"}</strong>
          <p>{entry.isCurrentUser ? "当前登录用户" : "训练用户"}</p>
        </div>
      </div>
      <div className="leaderboard-score">
        {formatMetricWithUnit(entry.score, metricUnit)}
      </div>
    </article>
  );
}

export default function LeaderboardView({ auth }) {
  const user = auth?.user ?? null;
  const tokens = auth?.tokens ?? null;
  const [leaderboardType, setLeaderboardType] = useState("DAILY_TOTAL");
  const [leaderboardStateByType, setLeaderboardStateByType] = useState(createLeaderboardStateByType);
  const pageSize = 20;

  const selectedState = leaderboardStateByType[leaderboardType] || createTabState();
  const leaderboardMeta = getLeaderboardMeta(leaderboardType);
  const fallbackMetric = getFallbackMetric(leaderboardType, user);
  const pageCount = selectedState.total > 0 ? Math.ceil(selectedState.total / pageSize) : 1;
  const isInitialLoading = selectedState.loading && !selectedState.hasLoaded;
  const isRefreshing = selectedState.loading && selectedState.hasLoaded;

  function updateLeaderboardState(type, updater) {
    setLeaderboardStateByType((prev) => {
      const current = prev[type] || createTabState();
      const next = typeof updater === "function"
        ? updater(current)
        : { ...current, ...updater };
      return {
        ...prev,
        [type]: next
      };
    });
  }

  async function loadPage(type, nextPage, refreshCurrentUser) {
    if (!tokens) {
      return;
    }

    updateLeaderboardState(type, (current) => ({
      ...current,
      loading: true,
      message: ""
    }));

    try {
      const pageResp = await getLeaderboard({ type, page: nextPage, pageSize }, tokens);
      if (pageResp.code !== 200) {
        updateLeaderboardState(type, (current) => ({
          ...current,
          loading: false,
          message: pageResp.message || "加载排行榜失败"
        }));
        return;
      }

      let currentUserEntry = pageResp.data?.currentUserEntry || null;
      if (refreshCurrentUser) {
        try {
          const meResp = await getMyLeaderboardRank(type, tokens);
          if (meResp.code === 200 && meResp.data) {
            currentUserEntry = meResp.data;
          }
        } catch {
          currentUserEntry = currentUserEntry || null;
        }
      }

      updateLeaderboardState(type, (current) => ({
        ...current,
        page: pageResp.data?.page || nextPage,
        total: pageResp.data?.total || 0,
        entries: pageResp.data?.entries || [],
        currentUserEntry,
        loading: false,
        message: "",
        hasLoaded: true
      }));
    } catch (error) {
      updateLeaderboardState(type, (current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载排行榜失败"
      }));
    }
  }

  useEffect(() => {
    if (!tokens) {
      return;
    }

    const cachedPage = leaderboardStateByType[leaderboardType]?.page || 1;
    void loadPage(leaderboardType, cachedPage, true);
  }, [tokens, user?.id, leaderboardType]);

  return (
    <section className="leaderboard-shell">
      <div className="leaderboard-toolbar">
        <div>
          <p className="section-eyebrow">Leaderboard</p>
          <h2>{leaderboardMeta.title}</h2>
          <p>{leaderboardMeta.description}</p>
        </div>

        <div className="leaderboard-type-tabs">
          {LEADERBOARD_TYPES.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`leaderboard-type-tab ${leaderboardType === item.key ? "is-active" : ""}`}
              onClick={() => setLeaderboardType(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {selectedState.message ? <p className="system-message">{selectedState.message}</p> : null}

      <article className="leaderboard-me-card">
        <div className="leaderboard-me-main">
          {selectedState.currentUserEntry?.avatarUrl ? (
            <img
              className="leaderboard-me-avatar"
              src={selectedState.currentUserEntry.avatarUrl}
              alt="avatar"
            />
          ) : (
            <span className="leaderboard-me-avatar leaderboard-me-avatar-fallback">
              {getUserInitial(user)}
            </span>
          )}
          <div>
            <span className="leaderboard-me-label">我的排名</span>
            <strong>{selectedState.currentUserEntry?.rank ?? "-"}</strong>
            <p>{selectedState.currentUserEntry?.username || user?.username || "-"}</p>
          </div>
        </div>

        <div className="leaderboard-me-score">
          <span>我的{leaderboardMeta.metricLabel}</span>
          <strong>
            {formatMetricWithUnit(
              selectedState.currentUserEntry?.score ?? fallbackMetric,
              leaderboardMeta.metricUnit
            )}
          </strong>
        </div>
      </article>

      <section className="leaderboard-list">
        <div className="leaderboard-list-head">
          <span>名次</span>
          <span>用户</span>
          <span>{leaderboardMeta.metricLabel}</span>
        </div>

        {isRefreshing ? (
          <p className="leaderboard-inline-status">正在刷新当前排行榜...</p>
        ) : null}

        {isInitialLoading ? (
          <article className="leaderboard-empty">正在加载排行榜...</article>
        ) : selectedState.entries.length === 0 ? (
          <article className="leaderboard-empty">当前暂无排行榜数据。</article>
        ) : (
          selectedState.entries.map((entry) => (
            <LeaderboardRow
              key={entry.userId ?? `${entry.rank}-${entry.username}`}
              entry={entry}
              metricUnit={leaderboardMeta.metricUnit}
            />
          ))
        )}
      </section>

      <div className="leaderboard-pagination">
        <button
          type="button"
          className="ghost-button"
          disabled={selectedState.loading || selectedState.page <= 1}
          onClick={() => void loadPage(leaderboardType, selectedState.page - 1, false)}
        >
          上一页
        </button>
        <span>
          第 {selectedState.page} / {pageCount} 页
        </span>
        <button
          type="button"
          className="ghost-button"
          disabled={selectedState.loading || selectedState.page >= pageCount}
          onClick={() => void loadPage(leaderboardType, selectedState.page + 1, false)}
        >
          下一页
        </button>
      </div>
    </section>
  );
}
