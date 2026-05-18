import { useEffect, useState } from "react";
import { getUserInitial } from "../auth";
import { regenerateTodayByAdmin } from "../api/dailyProblem";
import {
  getAdminDailyRecordDetail,
  getAdminDailyRecords,
  getAdminTrainingOverview,
  getAdminUserTimeline,
  getAdminUserTrainingPage
} from "../api/adminTraining";
import AdminAiProblemWorkbench from "./AdminAiProblemWorkbench";

function formatNumber(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function formatAverage(value) {
  return Number.isFinite(Number(value)) ? Number(value).toFixed(1) : "0.0";
}

function formatDateLabel(value) {
  if (!value) {
    return "-";
  }
  try {
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    }).format(new Date(`${value}T00:00:00`));
  } catch {
    return value;
  }
}

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

function toDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

function createEmptyPage(pageSize) {
  return {
    loading: false,
    message: "",
    page: 1,
    pageSize,
    total: 0,
    entries: []
  };
}

function AdminMetricCard({ label, value, detail, accent = false }) {
  return (
    <article className={`overview-stat ${accent ? "is-accent" : ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function UserAvatar({ username, avatarUrl }) {
  if (avatarUrl) {
    return <img className="leaderboard-avatar" src={avatarUrl} alt="avatar" />;
  }
  return (
    <span className="leaderboard-avatar leaderboard-avatar-fallback">
      {getUserInitial({ username })}
    </span>
  );
}

function ProblemSummaryCard({ problem, title, emptyText }) {
  if (!problem) {
    return (
      <article className="section-card">
        <div className="section-heading">
          <p className="section-eyebrow">Admin</p>
          <h3>{title}</h3>
        </div>
        <p className="empty-copy">{emptyText}</p>
      </article>
    );
  }

  return (
    <article className="section-card">
      <div className="section-heading">
        <p className="section-eyebrow">Admin</p>
        <h3>{title}</h3>
      </div>
      <div className="problem-card problem-card-embedded">
        <div className="problem-card-head">
          <div>
            <p className="section-eyebrow">Problem</p>
            <h3>
              {problem.contestId}
              {problem.problemIndex}. {problem.name}
            </h3>
          </div>
          <span className="problem-rating-badge">{problem.rating ?? "Unrated"}</span>
        </div>
        <div className="problem-meta-grid">
          <span>日期: {problem.date || "-"}</span>
          <span>类型: {problem.type || "-"}</span>
        </div>
        {problem.tags ? <p className="empty-copy">Tags: {problem.tags}</p> : null}
        {problem.sourceUrl ? (
          <a className="text-link" href={problem.sourceUrl} target="_blank" rel="noreferrer">
            打开题面
          </a>
        ) : null}
      </div>
    </article>
  );
}

export default function AdminTrainingView({ auth }) {
  const tokens = auth?.tokens ?? null;
  const [overviewState, setOverviewState] = useState({
    loading: false,
    message: "",
    data: null
  });
  const [dailyStartDate, setDailyStartDate] = useState(() => {
    const base = new Date();
    base.setDate(base.getDate() - 13);
    return toDateInputValue(base);
  });
  const [dailyEndDate, setDailyEndDate] = useState(() => toDateInputValue(new Date()));
  const [dailyState, setDailyState] = useState(createEmptyPage(8));
  const [selectedDailyDate, setSelectedDailyDate] = useState("");
  const [dailyDetailState, setDailyDetailState] = useState({
    loading: false,
    message: "",
    data: null
  });
  const [userKeyword, setUserKeyword] = useState("");
  const [userState, setUserState] = useState(createEmptyPage(8));
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [userTimelineState, setUserTimelineState] = useState({
    loading: false,
    message: "",
    data: null
  });
  const [actionBusy, setActionBusy] = useState(false);
  const [actionMessage, setActionMessage] = useState("");

  const selectedDailyRecord = dailyState.entries.find((item) => item.date === selectedDailyDate) || null;
  const selectedUserRecord = userState.entries.find((item) => item.userId === selectedUserId) || null;
  const dailyPageCount = dailyState.total > 0 ? Math.ceil(dailyState.total / dailyState.pageSize) : 1;
  const userPageCount = userState.total > 0 ? Math.ceil(userState.total / userState.pageSize) : 1;

  useEffect(() => {
    if (!tokens) {
      return;
    }

    void reloadOverview();
    void reloadDailyRecords(1);
    void reloadUsers(1);
  }, [tokens]);

  useEffect(() => {
    if (!tokens || !selectedDailyDate) {
      return;
    }
    void reloadDailyDetail(selectedDailyDate);
  }, [tokens, selectedDailyDate]);

  useEffect(() => {
    if (!tokens || !selectedUserId) {
      return;
    }
    void reloadUserTimeline(selectedUserId);
  }, [tokens, selectedUserId]);

  async function reloadOverview() {
    if (!tokens) {
      return;
    }

    setOverviewState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));

    try {
      const resp = await getAdminTrainingOverview(tokens);
      if (resp.code === 200) {
        setOverviewState({
          loading: false,
          message: "",
          data: resp.data || null
        });
      } else {
        setOverviewState((current) => ({
          ...current,
          loading: false,
          message: resp.message || "加载概览失败"
        }));
      }
    } catch (error) {
      setOverviewState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载概览失败"
      }));
    }
  }

  async function reloadDailyRecords(nextPage) {
    if (!tokens) {
      return;
    }

    setDailyState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));

    try {
      const resp = await getAdminDailyRecords(
        {
          startDate: dailyStartDate,
          endDate: dailyEndDate,
          page: nextPage,
          pageSize: dailyState.pageSize
        },
        tokens
      );

      if (resp.code !== 200) {
        setDailyState((current) => ({
          ...current,
          loading: false,
          message: resp.message || "加载每日明细失败"
        }));
        return;
      }

      const entries = resp.data?.entries || [];
      const nextDate = entries.some((item) => item.date === selectedDailyDate)
        ? selectedDailyDate
        : entries[0]?.date || "";

      setDailyState((current) => ({
        ...current,
        loading: false,
        message: "",
        page: resp.data?.page || nextPage,
        pageSize: resp.data?.pageSize || current.pageSize,
        total: resp.data?.total || 0,
        entries
      }));

      if (nextDate !== selectedDailyDate) {
        setSelectedDailyDate(nextDate);
      }
      if (entries.length === 0) {
        setDailyDetailState({
          loading: false,
          message: "",
          data: null
        });
      }
    } catch (error) {
      setDailyState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载每日明细失败"
      }));
    }
  }

  async function reloadDailyDetail(date) {
    if (!tokens || !date) {
      return;
    }

    setDailyDetailState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));

    try {
      const resp = await getAdminDailyRecordDetail(date, tokens);
      if (resp.code === 200) {
        setDailyDetailState({
          loading: false,
          message: "",
          data: resp.data || null
        });
      } else {
        setDailyDetailState((current) => ({
          ...current,
          loading: false,
          message: resp.message || "加载日期详情失败"
        }));
      }
    } catch (error) {
      setDailyDetailState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载日期详情失败"
      }));
    }
  }

  async function reloadUsers(nextPage) {
    if (!tokens) {
      return;
    }

    setUserState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));

    try {
      const resp = await getAdminUserTrainingPage(
        {
          keyword: userKeyword || undefined,
          page: nextPage,
          pageSize: userState.pageSize
        },
        tokens
      );

      if (resp.code !== 200) {
        setUserState((current) => ({
          ...current,
          loading: false,
          message: resp.message || "加载用户列表失败"
        }));
        return;
      }

      const entries = resp.data?.entries || [];
      const nextUserId = entries.some((item) => item.userId === selectedUserId)
        ? selectedUserId
        : entries[0]?.userId || null;

      setUserState((current) => ({
        ...current,
        loading: false,
        message: "",
        page: resp.data?.page || nextPage,
        pageSize: resp.data?.pageSize || current.pageSize,
        total: resp.data?.total || 0,
        entries
      }));

      if (nextUserId !== selectedUserId) {
        setSelectedUserId(nextUserId);
      }
      if (entries.length === 0) {
        setUserTimelineState({
          loading: false,
          message: "",
          data: null
        });
      }
    } catch (error) {
      setUserState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载用户列表失败"
      }));
    }
  }

  async function reloadUserTimeline(userId) {
    if (!tokens || !userId) {
      return;
    }

    setUserTimelineState((current) => ({
      ...current,
      loading: true,
      message: ""
    }));

    try {
      const resp = await getAdminUserTimeline(userId, { limit: 20 }, tokens);
      if (resp.code === 200) {
        setUserTimelineState({
          loading: false,
          message: "",
          data: resp.data || null
        });
      } else {
        setUserTimelineState((current) => ({
          ...current,
          loading: false,
          message: resp.message || "加载用户轨迹失败"
        }));
      }
    } catch (error) {
      setUserTimelineState((current) => ({
        ...current,
        loading: false,
        message: error.response?.data?.message || "加载用户轨迹失败"
      }));
    }
  }

  async function handleRegenerate() {
    if (!tokens || actionBusy) {
      return;
    }

    setActionBusy(true);
    setActionMessage("");
    try {
      const resp = await regenerateTodayByAdmin(tokens);
      if (resp.code !== 200) {
        setActionMessage(resp.message || "重新生成今日题目失败");
        return;
      }

      setActionMessage("今日题目已重新生成。");
      await reloadOverview();
      await reloadDailyRecords(dailyState.page);
      if (selectedDailyDate) {
        await reloadDailyDetail(selectedDailyDate);
      }
    } catch (error) {
      setActionMessage(error.response?.data?.message || "重新生成今日题目失败");
    } finally {
      setActionBusy(false);
    }
  }

  return (
    <section className="admin-view">
      <article className="section-card admin-hero-card">
        <div className="section-heading section-heading-inline">
          <div>
            <p className="section-eyebrow">Admin Dashboard</p>
            <h2>训练数据看板</h2>
            <p>按日期和用户查看训练行为、打卡明细、连续打卡表现与练习轨迹。</p>
          </div>
          <button className="primary-button" type="button" disabled={actionBusy} onClick={handleRegenerate}>
            {actionBusy ? "处理中..." : "重生今日题目"}
          </button>
        </div>
        {actionMessage ? <p className="system-message">{actionMessage}</p> : null}
      </article>

      <section className="overview-stat-grid admin-overview-grid">
        <AdminMetricCard
          label="总用户数"
          value={formatNumber(overviewState.data?.totalUsers)}
          detail="当前系统内全部账号"
          accent
        />
        <AdminMetricCard
          label="今日打卡"
          value={formatNumber(overviewState.data?.dailyCheckInCount)}
          detail={`活跃用户 ${formatNumber(overviewState.data?.activeUsers)}`}
        />
        <AdminMetricCard
          label="今日待打卡"
          value={formatNumber(overviewState.data?.pendingDailyUserCount)}
          detail="尚未完成今日打卡"
        />
        <AdminMetricCard
          label="今日练习校验"
          value={formatNumber(overviewState.data?.practiceCheckCount)}
          detail={`抽题 ${formatNumber(overviewState.data?.practiceDrawCount)} 次`}
        />
        <AdminMetricCard
          label="今日连续打卡"
          value={formatNumber(overviewState.data?.todayStreakUserCount)}
          detail="最近一次打卡日期就是今天"
        />
        <AdminMetricCard
          label="当前最高连续"
          value={`${formatNumber(overviewState.data?.maxCurrentStreakDays)} 天`}
          detail="按当前连续打卡天数统计"
        />
        <AdminMetricCard
          label="历史最高连续"
          value={`${formatNumber(overviewState.data?.maxLongestStreakDays)} 天`}
          detail="按历史最长连续打卡天数统计"
        />
        <AdminMetricCard
          label="平均当前连续"
          value={`${formatAverage(overviewState.data?.averageCurrentStreakDays)} 天`}
          detail="已打卡用户平均当前连续天数"
        />
      </section>

      {overviewState.message ? <p className="system-message">{overviewState.message}</p> : null}
      {overviewState.loading ? <p className="system-message is-loading">正在加载概览...</p> : null}

      <div className="admin-grid">
        <div className="admin-column">
          <ProblemSummaryCard
            title="今日训练概览"
            problem={overviewState.data?.problem}
            emptyText={overviewState.loading ? "正在加载今日题目..." : "今日暂无题目数据。"}
          />

          <section className="section-card">
            <div className="section-heading section-heading-inline">
              <div>
                <p className="section-eyebrow">By Date</p>
                <h3>每日训练明细</h3>
              </div>
              <div className="admin-filter-row">
                <input
                  className="auth-input"
                  type="date"
                  value={dailyStartDate}
                  onChange={(event) => setDailyStartDate(event.target.value)}
                />
                <input
                  className="auth-input"
                  type="date"
                  value={dailyEndDate}
                  onChange={(event) => setDailyEndDate(event.target.value)}
                />
                <button className="ghost-button" type="button" onClick={() => void reloadDailyRecords(1)}>
                  筛选
                </button>
              </div>
            </div>

            {dailyState.message ? <p className="system-message">{dailyState.message}</p> : null}
            {dailyState.loading ? <p className="system-message is-loading">正在加载每日明细...</p> : null}

            <div className="admin-table">
              <div className="admin-table-head">
                <span>日期</span>
                <span>题目</span>
                <span>打卡 / 练习</span>
                <span>查看</span>
              </div>
              {dailyState.entries.length === 0 ? (
                <p className="leaderboard-empty">当前范围内没有每日训练数据。</p>
              ) : (
                dailyState.entries.map((item) => (
                  <button
                    key={item.date}
                    type="button"
                    className={`admin-table-row ${selectedDailyDate === item.date ? "is-active" : ""}`}
                    onClick={() => setSelectedDailyDate(item.date)}
                  >
                    <span>{formatDateLabel(item.date)}</span>
                    <span>{item.problem?.name || "-"}</span>
                    <span>
                      {item.dailyCheckInCount} / {item.totalUsers}
                      <small>
                        练习校验 {item.practiceCheckCount}，抽题 {item.practiceDrawCount}
                      </small>
                    </span>
                    <span>查看</span>
                  </button>
                ))
              )}
            </div>

            <div className="leaderboard-pagination">
              <button
                type="button"
                className="ghost-button"
                disabled={dailyState.loading || dailyState.page <= 1}
                onClick={() => void reloadDailyRecords(dailyState.page - 1)}
              >
                上一页
              </button>
              <span>
                第 {dailyState.page} / {dailyPageCount} 页
              </span>
              <button
                type="button"
                className="ghost-button"
                disabled={dailyState.loading || dailyState.page >= dailyPageCount}
                onClick={() => void reloadDailyRecords(dailyState.page + 1)}
              >
                下一页
              </button>
            </div>

            <article className="section-card admin-detail-card">
              <div className="section-heading">
                <p className="section-eyebrow">Selected Day</p>
                <h3>{selectedDailyRecord?.date ? formatDateLabel(selectedDailyRecord.date) : "请选择日期"}</h3>
              </div>
              {dailyDetailState.message ? <p className="system-message">{dailyDetailState.message}</p> : null}
              {dailyDetailState.loading ? <p className="system-message is-loading">正在加载日期详情...</p> : null}
              {dailyDetailState.data ? (
                <div className="admin-detail-stack">
                  <ProblemSummaryCard
                    title="题目详情"
                    problem={dailyDetailState.data.problem}
                    emptyText="暂无题目"
                  />
                  <div className="admin-detail-columns">
                    <article className="admin-mini-panel">
                      <div className="section-heading">
                        <p className="section-eyebrow">Daily Check-Ins</p>
                        <h3>打卡明细</h3>
                      </div>
                      {dailyDetailState.data.checkIns.length === 0 ? (
                        <p className="empty-copy">暂无打卡记录。</p>
                      ) : (
                        <div className="admin-entity-list">
                          {dailyDetailState.data.checkIns.map((item) => (
                            <div className="admin-entity-row" key={`${item.userId}-${item.submissionId}`}>
                              <div className="admin-entity-user">
                                <UserAvatar username={item.username} avatarUrl={item.avatarUrl} />
                                <strong>{item.username}</strong>
                              </div>
                              <span>
                                submission {item.submissionId}
                                <small>
                                  {item.verdict} / +{item.score ?? 0}
                                </small>
                              </span>
                              <small>{formatDateTimeLabel(item.checkedAt)}</small>
                            </div>
                          ))}
                        </div>
                      )}
                    </article>

                    <article className="admin-mini-panel">
                      <div className="section-heading">
                        <p className="section-eyebrow">Practice Checks</p>
                        <h3>练习明细</h3>
                      </div>
                      {dailyDetailState.data.practiceChecks.length === 0 ? (
                        <p className="empty-copy">暂无练习记录。</p>
                      ) : (
                        <div className="admin-entity-list">
                          {dailyDetailState.data.practiceChecks.map((item) => (
                            <div className="admin-entity-row" key={item.drawId}>
                              <div className="admin-entity-user">
                                <UserAvatar username={item.username} avatarUrl={item.avatarUrl} />
                                <strong>{item.username}</strong>
                              </div>
                              <span>
                                {item.problemKey}
                                <small>
                                  {item.verdict} / submission {item.submissionId}
                                </small>
                              </span>
                              <small>{formatDateTimeLabel(item.checkedAt)}</small>
                            </div>
                          ))}
                        </div>
                      )}
                    </article>
                  </div>
                </div>
              ) : (
                <p className="empty-copy">请选择一条日期记录查看详情。</p>
              )}
            </article>
          </section>
        </div>

        <div className="admin-column">
          <section className="section-card">
            <div className="section-heading section-heading-inline">
              <div>
                <p className="section-eyebrow">By User</p>
                <h3>用户训练轨迹</h3>
              </div>
              <div className="admin-filter-row">
                <input
                  className="auth-input"
                  value={userKeyword}
                  onChange={(event) => setUserKeyword(event.target.value)}
                  placeholder="按用户名或邮箱检索"
                />
                <button className="ghost-button" type="button" onClick={() => void reloadUsers(1)}>
                  搜索
                </button>
              </div>
            </div>

            {userState.message ? <p className="system-message">{userState.message}</p> : null}
            {userState.loading ? <p className="system-message is-loading">正在加载用户列表...</p> : null}

            <div className="admin-table">
              <div className="admin-table-head is-user-table">
                <span>用户</span>
                <span>积分 / 做题</span>
                <span>连续打卡</span>
                <span>最近活动</span>
                <span>查看</span>
              </div>
              {userState.entries.length === 0 ? (
                <p className="leaderboard-empty">当前没有匹配的用户数据。</p>
              ) : (
                userState.entries.map((item) => (
                  <button
                    key={item.userId}
                    type="button"
                    className={`admin-table-row is-user-table ${selectedUserId === item.userId ? "is-active" : ""}`}
                    onClick={() => setSelectedUserId(item.userId)}
                  >
                    <span>
                      {item.username}
                      <small>{item.role || "-"}</small>
                    </span>
                    <span>
                      {formatNumber(item.score)}
                      <small>
                        做题 {formatNumber(item.solvedProblemCount)}，难题 {formatNumber(item.hardSolvedProblemCount)}
                      </small>
                    </span>
                    <span>
                      当前 {formatNumber(item.currentStreakDays)} 天
                      <small>历史最长 {formatNumber(item.longestStreakDays)} 天</small>
                    </span>
                    <span>
                      {item.lastDailyDate ? formatDateLabel(item.lastDailyDate) : "-"}
                      <small>
                        打卡 {item.dailyCheckInCount} / 练习 {item.practiceCheckCount}
                        <br />
                        最近练习 {formatDateTimeLabel(item.lastPracticeCheckedAt)}
                      </small>
                    </span>
                    <span>查看</span>
                  </button>
                ))
              )}
            </div>

            <div className="leaderboard-pagination">
              <button
                type="button"
                className="ghost-button"
                disabled={userState.loading || userState.page <= 1}
                onClick={() => void reloadUsers(userState.page - 1)}
              >
                上一页
              </button>
              <span>
                第 {userState.page} / {userPageCount} 页
              </span>
              <button
                type="button"
                className="ghost-button"
                disabled={userState.loading || userState.page >= userPageCount}
                onClick={() => void reloadUsers(userState.page + 1)}
              >
                下一页
              </button>
            </div>

            <article className="section-card admin-detail-card">
              <div className="section-heading">
                <p className="section-eyebrow">Timeline</p>
                <h3>{selectedUserRecord?.username || "请选择用户"}</h3>
              </div>
              {userTimelineState.message ? <p className="system-message">{userTimelineState.message}</p> : null}
              {userTimelineState.loading ? <p className="system-message is-loading">正在加载用户轨迹...</p> : null}
              {userTimelineState.data ? (
                <div className="admin-detail-stack">
                  <div className="admin-user-summary">
                    <strong>{userTimelineState.data.username || "-"}</strong>
                    <p>
                      积分 {formatNumber(userTimelineState.data.score)} / 做题 {formatNumber(userTimelineState.data.solvedProblemCount)}
                      / 难题 {formatNumber(userTimelineState.data.hardSolvedProblemCount)}
                    </p>
                    <p>
                      当前连续 {formatNumber(userTimelineState.data.currentStreakDays)} 天 / 历史最长 {formatNumber(userTimelineState.data.longestStreakDays)} 天
                    </p>
                  </div>
                  {userTimelineState.data.entries.length === 0 ? (
                    <p className="empty-copy">暂无轨迹记录。</p>
                  ) : (
                    <div className="admin-entity-list">
                      {userTimelineState.data.entries.map((item) => (
                        <div className="admin-entity-row" key={`${item.activityType}-${item.activityAt}-${item.drawId || item.submissionId}`}>
                          <div className="admin-entity-user">
                            <span className="admin-badge">{item.activityType}</span>
                            <strong>{item.problemKey}</strong>
                          </div>
                          <span>
                            {item.name}
                            <small>
                              {item.verdict} / {item.rating ?? "Unrated"} / +{item.score ?? 0}
                            </small>
                          </span>
                          <small>{formatDateTimeLabel(item.activityAt)}</small>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <p className="empty-copy">请选择一个用户查看完整轨迹。</p>
              )}
            </article>
          </section>
        </div>
      </div>

      <AdminAiProblemWorkbench auth={auth} />
    </section>
  );
}
