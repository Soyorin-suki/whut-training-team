import { useEffect, useState } from "react";
import { getUserInitial } from "../auth";
import {
  checkInToday,
  checkPractice,
  drawPracticeProblem,
  getDailyHistory,
  getPracticeHistory,
  getTodayProblem,
  regenerateTodayByAdmin
} from "../api/dailyProblem";
import { getLeaderboard, getMyLeaderboardRank } from "../api/rankings";
import { logout, updateMyProfile } from "../api/user";
import AdminTrainingView from "./AdminTrainingView";
import LeaderboardView from "./LeaderboardView";

const PRACTICE_TAG_OPTIONS = [
  "implementation",
  "greedy",
  "math",
  "dp",
  "graphs",
  "data structures",
  "brute force",
  "constructive algorithms",
  "sortings",
  "binary search",
  "strings",
  "number theory"
];

const NAV_ITEMS = [
  { key: "overview", label: "门户首页" },
  { key: "daily", label: "每日一题" },
  { key: "practice", label: "自主练习" },
  { key: "leaderboard", label: "积分榜" },
  { key: "profile", label: "个人中心" }
];

const ADMIN_NAV_ITEM = { key: "admin", label: "管理看板" };

const QUICK_LINKS = [
  {
    key: "daily",
    index: "01",
    title: "进入每日训练",
    description: "查看今天的统一题目并提交打卡。"
  },
  {
    key: "practice",
    index: "02",
    title: "开始自主补题",
    description: "按难度和标签抽题，集中攻克薄弱专题。"
  },
  {
    key: "leaderboard",
    index: "03",
    title: "查看积分榜",
    description: "追踪当前排名与积分差距。"
  },
  {
    key: "profile",
    index: "04",
    title: "维护个人资料",
    description: "同步 handle、邮箱和训练身份信息。"
  }
];

const ANNOUNCEMENTS = [
  {
    title: "训练说明",
    body: "每日题用于统一打卡，自主练习用于补专题，不参与统一积分。"
  },
  {
    title: "打卡规则",
    body: "输入 Codeforces 提交 ID 后系统会校验 verdict，并按规则记分。"
  },
  {
    title: "账号同步",
    body: "修改用户名后会同步刷新 Codeforces handle 对应的段位信息。"
  }
];

const RESOURCES = [
  { label: "Codeforces", href: "https://codeforces.com/" },
  { label: "AtCoder", href: "https://atcoder.jp/" },
  { label: "Luogu", href: "https://www.luogu.com.cn/" }
];

function parseTags(tags) {
  if (!tags) {
    return [];
  }

  return String(tags)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 6);
}

function getRatingMeta(rating) {
  if (rating === null || rating === undefined) {
    return { label: "Unrated", color: "#68707f" };
  }
  if (rating < 1200) return { label: "Newbie", color: "#68707f" };
  if (rating < 1400) return { label: "Pupil", color: "#32824a" };
  if (rating < 1600) return { label: "Specialist", color: "#0f8a8d" };
  if (rating < 1900) return { label: "Expert", color: "#2b59d9" };
  if (rating < 2100) return { label: "Candidate Master", color: "#9156d5" };
  if (rating < 2400) return { label: "Master", color: "#da6d00" };
  return { label: "Grandmaster", color: "#c53030" };
}

function formatOnlineText(online) {
  if (online === null || online === undefined) {
    return "未知";
  }
  return online ? "在线" : "离线";
}

function formatScore(score) {
  return Number.isFinite(Number(score)) ? Number(score) : 0;
}

function getDailyStatus(todayData) {
  if (!todayData?.problem) {
    return { label: "未生成", tone: "muted" };
  }
  if (todayData.checkedIn) {
    return { label: "已打卡", tone: "success" };
  }
  return { label: "待完成", tone: "warning" };
}

function OverviewStat({ label, value, detail, accent = false }) {
  return (
    <article className={`overview-stat ${accent ? "is-accent" : ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function ProblemCard({ problem, title = "题目详情", emptyText = "当前暂无题目数据。" }) {
  if (!problem) {
    return (
      <article className="section-card empty-card">
        <div className="section-heading">
          <p className="section-eyebrow">Problem</p>
          <h3>{title}</h3>
        </div>
        <p className="empty-copy">{emptyText}</p>
      </article>
    );
  }

  return (
    <article className="section-card problem-card">
      <div className="problem-card-head">
        <div>
          <p className="section-eyebrow">Problem</p>
          <h3>
            {problem.contestId}
            {problem.problemIndex}. {problem.name}
          </h3>
        </div>
        <span className="problem-rating-badge">{problem.rating ?? "未定级"}</span>
      </div>

      <div className="problem-meta-grid">
        <span>日期：{problem.date || "-"}</span>
        <span>类型：{problem.type || "-"}</span>
      </div>

      <div className="tag-list">
        {parseTags(problem.tags).map((tag) => (
          <span className="tag" key={tag}>
            {tag}
          </span>
        ))}
      </div>

      <a className="text-link" href={problem.sourceUrl} target="_blank" rel="noreferrer">
        前往 Codeforces 查看题面
      </a>
    </article>
  );
}

function EmbeddedProblemCard({ problem, title = "题目详情", emptyText = "当前暂无题目数据。" }) {
  if (!problem) {
    return (
      <div className="problem-card problem-card-embedded">
        <div className="section-heading">
          <p className="section-eyebrow">Problem</p>
          <h3>{title}</h3>
        </div>
        <p className="empty-copy">{emptyText}</p>
      </div>
    );
  }

  return (
    <div className="problem-card problem-card-embedded">
      <div className="problem-card-head">
        <div>
          <p className="section-eyebrow">Problem</p>
          <h3>
            {problem.contestId}
            {problem.problemIndex}. {problem.name}
          </h3>
        </div>
        <span className="problem-rating-badge">{problem.rating ?? "未定级"}</span>
      </div>

      <div className="problem-meta-grid">
        <span>日期：{problem.date || "-"}</span>
        <span>类型：{problem.type || "-"}</span>
      </div>

      <div className="tag-list">
        {parseTags(problem.tags).map((tag) => (
          <span className="tag" key={tag}>
            {tag}
          </span>
        ))}
      </div>

      <a className="text-link" href={problem.sourceUrl} target="_blank" rel="noreferrer">
        前往 Codeforces 查看题面
      </a>
    </div>
  );
}

function HistoryTimeline({ title, items, type, emptyText }) {
  return (
    <section className="section-card history-card">
      <div className="section-heading">
        <p className="section-eyebrow">History</p>
        <h3>{title}</h3>
      </div>

      {items.length === 0 ? (
        <p className="empty-copy">{emptyText}</p>
      ) : (
        <div className="timeline-list">
          {items.map((item) => {
            const isDaily = type === "daily";
            const date = isDaily ? item.date : item.drawDate;
            const status = isDaily
              ? item.checkedIn
                ? `已打卡 · submission ${item.submissionId ?? "-"}`
                : "未打卡"
              : `已完成 · submission ${item.submissionId ?? "-"}`;

            return (
              <article
                className={`timeline-item ${isDaily && item.checkedIn ? "is-success" : ""}`}
                key={isDaily ? `${item.date}-${item.problemKey}` : item.drawId}
              >
                <div className="timeline-date">{date || "-"}</div>
                <div className="timeline-body">
                  <strong>
                    {item.problemKey} · {item.name}
                  </strong>
                  <p>
                    rating {item.rating ?? "未定级"} · verdict {item.verdict || "-"}
                    {isDaily && item.checkedIn ? ` · +${item.score ?? 0}` : ""}
                  </p>
                </div>
                <div className="timeline-status">{status}</div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function RankingPreview({ entries, currentUserEntry, user, loading, onOpenFull }) {
  return (
    <section className="section-card ranking-preview-card">
      <div className="section-heading section-heading-inline">
        <div>
          <p className="section-eyebrow">Ranking</p>
          <h3>积分榜预览</h3>
        </div>
        <button className="ghost-button" type="button" onClick={onOpenFull}>
          查看完整榜单
        </button>
      </div>

      <article className="rank-me-card">
        <div>
          <span>我的当前排名</span>
          <strong>{currentUserEntry?.rank ?? "-"}</strong>
          <small>{currentUserEntry?.username || user?.username || "-"}</small>
        </div>
        <div className="rank-me-score">
          <span>积分</span>
          <strong>{formatScore(currentUserEntry?.score ?? user?.score)}</strong>
        </div>
      </article>

      {loading ? (
        <p className="empty-copy">正在加载排行榜...</p>
      ) : entries.length === 0 ? (
        <p className="empty-copy">当前暂无排行榜数据。</p>
      ) : (
        <div className="mini-rank-list">
          {entries.map((entry) => (
            <article className="mini-rank-row" key={entry.userId ?? `${entry.rank}-${entry.username}`}>
              <span className="mini-rank-no">{entry.rank ?? "-"}</span>
              <div className="mini-rank-user">
                <strong>{entry.username || "-"}</strong>
                <small>{entry.isCurrentUser ? "我自己" : "训练用户"}</small>
              </div>
              <span className="mini-rank-score">{formatScore(entry.score)}</span>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function QuickLinkGrid({ onOpen }) {
  return (
    <section className="quick-link-grid">
      {QUICK_LINKS.map((item) => (
        <button className="quick-link-card" key={item.key} type="button" onClick={() => onOpen(item.key)}>
          <span className="quick-link-index">{item.index}</span>
          <strong>{item.title}</strong>
          <p>{item.description}</p>
        </button>
      ))}
    </section>
  );
}

export default function MainView({ auth, onLogout, onNavigate, onUserUpdate }) {
  const [activeNav, setActiveNav] = useState("overview");
  const [actionBusy, setActionBusy] = useState(false);
  const [dailyPanelLoading, setDailyPanelLoading] = useState(false);
  const [practiceHistoryLoading, setPracticeHistoryLoading] = useState(false);
  const [rankingPreviewLoading, setRankingPreviewLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [profileEditMode, setProfileEditMode] = useState(false);

  const [todayData, setTodayData] = useState(null);
  const [history, setHistory] = useState([]);
  const [dailySubmissionId, setDailySubmissionId] = useState("");

  const [practiceMinRating, setPracticeMinRating] = useState(1200);
  const [practiceMaxRating, setPracticeMaxRating] = useState(1600);
  const [practiceTags, setPracticeTags] = useState("");
  const [selectedPracticeTag, setSelectedPracticeTag] = useState(PRACTICE_TAG_OPTIONS[0]);
  const [isDrawingPractice, setIsDrawingPractice] = useState(false);
  const [practiceDraw, setPracticeDraw] = useState(null);
  const [practiceSubmissionId, setPracticeSubmissionId] = useState("");
  const [practiceHistory, setPracticeHistory] = useState([]);

  const [previewEntries, setPreviewEntries] = useState([]);
  const [currentRankEntry, setCurrentRankEntry] = useState(null);

  const [profileUsername, setProfileUsername] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [profilePassword, setProfilePassword] = useState("");

  const user = auth?.user ?? null;
  const tokens = auth?.tokens ?? null;
  const isAdmin = user?.role === "ADMIN";
  const ratingMeta = getRatingMeta(user?.codeforcesRating);
  const dailyStatus = getDailyStatus(todayData);
  const navItems = isAdmin ? [...NAV_ITEMS, ADMIN_NAV_ITEM] : NAV_ITEMS;
  const showAdminPage = isAdmin && activeNav === "admin";

  const normalizedProfileUsername = profileUsername.trim();
  const normalizedProfileEmail = profileEmail.trim();
  const usernameHasSpace = /\s/.test(profileUsername);
  const hasTextProfileChanges = Boolean(
    user
      && (
        normalizedProfileUsername !== (user.username || "")
        || normalizedProfileEmail !== (user.email || "")
        || profilePassword.trim()
      )
  );
  const saveDisabled = actionBusy || !hasTextProfileChanges || !normalizedProfileUsername || usernameHasSpace;

  useEffect(() => {
    setProfileUsername(user?.username || "");
    setProfileEmail(user?.email || "");
    setProfilePassword("");
    setProfileEditMode(false);
  }, [user?.id]);

  useEffect(() => {
    if (!user || !tokens) {
      return;
    }

    void loadDailyPanel();
    void loadPracticeHistory();
    void loadRankingPreview();
  }, [tokens, user?.id]);

  useEffect(() => {
    if (!user || !tokens) {
      return;
    }

    if (activeNav === "daily") {
      void loadDailyPanel();
    }
    if (activeNav === "practice") {
      void loadPracticeHistory();
    }
    if (activeNav === "overview") {
      void loadRankingPreview();
    }
  }, [activeNav, tokens, user?.id]);

  useEffect(() => {
    if (!isAdmin && activeNav === "admin") {
      setActiveNav("overview");
    }
  }, [activeNav, isAdmin]);

  async function loadDailyPanel() {
    if (!tokens) {
      return;
    }

    setDailyPanelLoading(true);
    try {
      const [todayResp, historyResp] = await Promise.all([
        getTodayProblem(tokens),
        getDailyHistory(tokens, 14)
      ]);

      if (todayResp.code === 200) {
        setTodayData(todayResp.data);
      } else {
        setMessage(todayResp.message || "获取今日题目失败");
      }

      if (historyResp.code === 200) {
        setHistory(historyResp.data || []);
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "加载每日训练数据失败");
    } finally {
      setDailyPanelLoading(false);
    }
  }

  async function loadPracticeHistory() {
    if (!tokens) {
      return;
    }

    setPracticeHistoryLoading(true);
    try {
      const historyResp = await getPracticeHistory(tokens, 30);
      if (historyResp.code === 200) {
        setPracticeHistory(historyResp.data || []);
      }
    } catch {
      // keep silent for background loading
    } finally {
      setPracticeHistoryLoading(false);
    }
  }

  async function loadRankingPreview() {
    if (!tokens) {
      return;
    }

    setRankingPreviewLoading(true);
    try {
      const [pageResp, meResp] = await Promise.all([
        getLeaderboard({ type: "DAILY_TOTAL", page: 1, pageSize: 5 }, tokens),
        getMyLeaderboardRank("DAILY_TOTAL", tokens)
      ]);

      if (pageResp.code === 200) {
        setPreviewEntries(pageResp.data?.entries || []);
      }
      if (meResp.code === 200) {
        setCurrentRankEntry(meResp.data || null);
      }
    } catch {
      // keep silent for dashboard preview
    } finally {
      setRankingPreviewLoading(false);
    }
  }

  async function handleLogout() {
    setActionBusy(true);
    try {
      if (tokens?.accessToken && tokens?.refreshToken) {
        await logout(tokens);
      }
    } catch {
      // ignore logout failure and clear local session
    } finally {
      setActionBusy(false);
      onLogout?.();
    }
  }

  async function handleDailyCheckIn() {
    if (!dailySubmissionId.trim()) {
      setMessage("请输入 Codeforces 提交 ID");
      return;
    }

    setActionBusy(true);
    setMessage("");
    try {
      const resp = await checkInToday(Number(dailySubmissionId), tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "打卡失败");
        return;
      }

      const result = resp.data || {};
      setMessage(
        `打卡成功：submission ${result.submissionId ?? "-"}，verdict ${result.verdict ?? "-"}，积分 +${result.score ?? 0}`
      );
      setDailySubmissionId("");
      await loadDailyPanel();
      await loadRankingPreview();
    } catch (error) {
      setMessage(error.response?.data?.message || "打卡请求失败");
    } finally {
      setActionBusy(false);
    }
  }

  async function handleRegenerate() {
    if (!isAdmin) {
      return;
    }

    setActionBusy(true);
    setMessage("");
    try {
      const resp = await regenerateTodayByAdmin(tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "重新生成失败");
        return;
      }

      setMessage("今日题目已重新生成");
      await loadDailyPanel();
    } catch (error) {
      setMessage(error.response?.data?.message || "重新生成请求失败");
    } finally {
      setActionBusy(false);
    }
  }

  async function handleDrawPractice() {
    if (isDrawingPractice) {
      return;
    }

    setIsDrawingPractice(true);
    setMessage("");
    try {
      const resp = await drawPracticeProblem(
        {
          minRating: Number(practiceMinRating),
          maxRating: Number(practiceMaxRating),
          tags: practiceTags.trim() || null
        },
        tokens
      );
      if (resp.code !== 200) {
        setMessage(resp.message || "抽题失败");
        return;
      }

      setPracticeDraw(resp.data);
      setPracticeSubmissionId("");
    } catch (error) {
      setMessage(error.response?.data?.message || "抽题请求失败");
    } finally {
      setIsDrawingPractice(false);
    }
  }

  function handleAddPresetTag() {
    if (!selectedPracticeTag) {
      return;
    }

    const currentTags = practiceTags
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);

    if (currentTags.some((tag) => tag.toLowerCase() === selectedPracticeTag.toLowerCase())) {
      return;
    }

    setPracticeTags(currentTags.length > 0 ? `${currentTags.join(",")},${selectedPracticeTag}` : selectedPracticeTag);
  }

  function normalizeToHundreds(value) {
    if (value === null || value === undefined || String(value).trim() === "") {
      return "";
    }
    const num = Number(value);
    if (!Number.isFinite(num)) {
      return "";
    }
    return Math.round(num / 100) * 100;
  }

  async function handlePracticeCheck() {
    if (!practiceDraw?.drawId) {
      setMessage("请先抽取练习题");
      return;
    }
    if (!practiceSubmissionId.trim()) {
      setMessage("请输入 Codeforces 提交 ID");
      return;
    }

    setActionBusy(true);
    setMessage("");
    try {
      const resp = await checkPractice(practiceDraw.drawId, Number(practiceSubmissionId), tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "练习校验失败");
        return;
      }

      if (resp.data?.accepted) {
        setMessage("练习题校验通过，本题不计入统一积分");
        await loadPracticeHistory();
      } else {
        setMessage(`练习题未通过，verdict 为 ${resp.data?.verdict || "-"}`);
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "练习题校验请求失败");
    } finally {
      setActionBusy(false);
    }
  }

  function enterProfileEditMode() {
    setProfileUsername(user?.username || "");
    setProfileEmail(user?.email || "");
    setProfilePassword("");
    setProfileEditMode(true);
    setMessage("");
  }

  function cancelProfileEditMode() {
    setProfileUsername(user?.username || "");
    setProfileEmail(user?.email || "");
    setProfilePassword("");
    setProfileEditMode(false);
    setMessage("");
  }

  async function handleSaveProfile() {
    if (!normalizedProfileUsername) {
      setMessage("用户名不能为空");
      return;
    }
    if (usernameHasSpace) {
      setMessage("用户名不能包含空格");
      return;
    }
    if (!hasTextProfileChanges) {
      setMessage("当前没有需要保存的修改");
      return;
    }

    setActionBusy(true);
    setMessage("");
    try {
      const payload = {
        username: normalizedProfileUsername,
        email: normalizedProfileEmail || null,
        password: profilePassword || null
      };

      const resp = await updateMyProfile(payload, tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "个人资料更新失败");
        return;
      }

      onUserUpdate?.(resp.data);
      setProfilePassword("");
      setProfileEditMode(false);
      setMessage("个人资料已更新");
    } catch (error) {
      setMessage(error.response?.data?.message || "个人资料更新请求失败");
    } finally {
      setActionBusy(false);
    }
  }

  if (!user) {
    return (
      <main className="portal-shell">
        <header className="site-header portal-header">
          <div className="site-brand">
            <span className="site-brand-mark">WHUT</span>
            <div>
              <strong>WHUT Training Portal</strong>
              <span>请先登录后使用完整训练功能</span>
            </div>
          </div>
          <div className="site-actions">
            <button className="ghost-button" type="button" onClick={() => onNavigate?.("login")}>
              登录
            </button>
            <button className="primary-button" type="button" onClick={() => onNavigate?.("register")}>
              注册
            </button>
          </div>
        </header>
      </main>
    );
  }

  return (
    <main className="portal-shell">
      <header className="site-header portal-header">
        <div className="site-brand">
          <span className="site-brand-mark">WHUT</span>
          <div>
            <strong>WHUT Training Portal</strong>
            <span>Campus algorithm training portal inspired by ACM team sites</span>
          </div>
        </div>

        <nav className="site-nav portal-nav">
          {navItems.map((item) => (
            <button
              className={`site-nav-link ${activeNav === item.key ? "is-active" : ""}`}
              key={item.key}
              type="button"
              onClick={() => setActiveNav(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="user-toolbar">
          <div className="user-chip">
            {user.avatarUrl ? (
              <img className="avatar-image" src={user.avatarUrl} alt="avatar" />
            ) : (
              <span className="avatar-badge">{getUserInitial(user)}</span>
            )}
            <div>
              <strong>{user.username || "-"}</strong>
              <span>{ratingMeta.label}</span>
            </div>
          </div>
          <button className="ghost-button" type="button" onClick={handleLogout} disabled={actionBusy}>
            退出登录
          </button>
        </div>
      </header>

      {showAdminPage ? (
        <AdminTrainingView auth={auth} />
      ) : (
      <section className="portal-page">
        <article className="portal-hero">
          <div className="portal-hero-copy">
            <p className="section-eyebrow">Dashboard</p>
            <h1>欢迎回来，{user.username || "同学"}。</h1>
            <p>
              这里把每日题、自主练习、积分榜和个人画像收拢到同一首页，保留训练系统的功能，同时改成更接近 ACM 门户的浏览方式。
            </p>
          </div>

          <div className="portal-hero-aside">
            <div className={`status-pill is-${dailyStatus.tone}`}>{dailyStatus.label}</div>
            <strong>{todayData?.problem?.name || "今日题目待加载"}</strong>
            <span>
              {todayData?.problem
                ? `${todayData.problem.contestId}${todayData.problem.problemIndex} · rating ${todayData.problem.rating ?? "未定级"}`
                : "加载完成后会在这里显示今日题目概览"}
            </span>
          </div>
        </article>

        <section className="overview-stat-grid">
          <OverviewStat
            label="当前积分"
            value={formatScore(user.score)}
            detail="统一积分榜实时累计"
            accent
          />
          <OverviewStat
            label="Codeforces Rating"
            value={user.codeforcesRating ?? "-"}
            detail={`段位 ${ratingMeta.label}`}
          />
          <OverviewStat
            label="每日状态"
            value={dailyStatus.label}
            detail={todayData?.checkedIn ? "今天已经完成打卡" : "今天还没有完成打卡"}
          />
          <OverviewStat
            label="练习记录"
            value={practiceHistory.length}
            detail="最近 30 条自主练习记录"
          />
        </section>

        {message && <p className="system-message">{message}</p>}
        {actionBusy && <p className="system-message is-loading">请求处理中...</p>}

        {activeNav === "overview" && (
          <div className="portal-grid">
            <div className="portal-main-column">
              <section className="section-card">
                <div className="section-heading">
                  <p className="section-eyebrow">Navigation</p>
                  <h2>训练入口</h2>
                  <p>按模块进入训练、补题、榜单和个人中心。</p>
                </div>
                <QuickLinkGrid onOpen={setActiveNav} />
              </section>

              <ProblemCard
                problem={todayData?.problem}
                title="今日训练"
                emptyText={dailyPanelLoading ? "正在加载今日题目..." : "今日暂无题目数据。"}
              />

              <div className="split-panel">
                <HistoryTimeline
                  title="最近每日打卡"
                  items={history.slice(0, 5)}
                  type="daily"
                  emptyText={dailyPanelLoading ? "正在加载打卡记录..." : "最近还没有每日打卡记录。"}
                />
                <HistoryTimeline
                  title="最近自主练习"
                  items={practiceHistory.slice(0, 5)}
                  type="practice"
                  emptyText={practiceHistoryLoading ? "正在加载练习记录..." : "最近还没有自主练习记录。"}
                />
              </div>
            </div>

            <div className="portal-side-column">
              <RankingPreview
                entries={previewEntries}
                currentUserEntry={currentRankEntry}
                user={user}
                loading={rankingPreviewLoading}
                onOpenFull={() => setActiveNav("leaderboard")}
              />

              <section className="section-card side-info-card">
                <div className="section-heading">
                  <p className="section-eyebrow">Announcements</p>
                  <h3>训练公告</h3>
                </div>
                <div className="notice-list">
                  {ANNOUNCEMENTS.map((item) => (
                    <article className="notice-item" key={item.title}>
                      <strong>{item.title}</strong>
                      <p>{item.body}</p>
                    </article>
                  ))}
                </div>
              </section>

              <section className="section-card side-info-card">
                <div className="section-heading">
                  <p className="section-eyebrow">Resources</p>
                  <h3>常用资源站</h3>
                </div>
                <div className="resource-list">
                  {RESOURCES.map((resource) => (
                    <a className="resource-link" href={resource.href} key={resource.label} target="_blank" rel="noreferrer">
                      <span>{resource.label}</span>
                      <small>Open</small>
                    </a>
                  ))}
                </div>
              </section>
            </div>
          </div>
        )}

        {activeNav === "daily" && (
          <div className="portal-grid single-column">
            <section className="section-card">
              <div className="section-heading">
                <p className="section-eyebrow">Daily Check-In</p>
                <h2>今日统一训练</h2>
                <p>同一天所有用户共享同一道题，提交正确的 Codeforces submission ID 后完成打卡。</p>
              </div>
              <div className="training-panel">
                <EmbeddedProblemCard
                problem={todayData?.problem}
                title="今日题目"
                emptyText={dailyPanelLoading ? "正在加载今日题目..." : "今日暂无题目数据。"}
              />
              <div className="action-strip">
                <input
                  className="auth-input inline-input"
                  value={dailySubmissionId}
                  onChange={(event) => setDailySubmissionId(event.target.value)}
                  placeholder="输入 Codeforces 提交 ID"
                />
                <button
                  className="primary-button"
                  type="button"
                  disabled={todayData?.checkedIn || actionBusy}
                  onClick={handleDailyCheckIn}
                >
                  {todayData?.checkedIn ? "今日已打卡" : "提交打卡"}
                </button>
                {isAdmin && (
                  <button className="ghost-button" type="button" disabled={actionBusy} onClick={handleRegenerate}>
                    管理员重新生成
                  </button>
                )}
              </div>
              </div>
            </section>

            <HistoryTimeline
              title="最近 14 天每日训练记录"
              items={history}
              type="daily"
              emptyText={dailyPanelLoading ? "正在加载记录..." : "暂时没有每日训练记录。"}
            />
          </div>
        )}

        {activeNav === "practice" && (
          <div className="portal-grid single-column">
            <section className="section-card">
              <div className="section-heading">
                <p className="section-eyebrow">Practice Drawer</p>
                <h2>自主补题</h2>
                <p>按评分区间和标签抽取一道训练题，用于专题补强，不计入统一积分榜。</p>
              </div>

              <div className="training-panel">
                <div className="filter-grid">
                <label className="field-stack">
                  <span>最低难度</span>
                  <input
                    className="auth-input"
                    type="number"
                    step="100"
                    value={practiceMinRating}
                    onChange={(event) => setPracticeMinRating(event.target.value)}
                    onBlur={(event) => setPracticeMinRating(normalizeToHundreds(event.target.value))}
                    placeholder="1200"
                  />
                </label>

                <label className="field-stack">
                  <span>最高难度</span>
                  <input
                    className="auth-input"
                    type="number"
                    step="100"
                    value={practiceMaxRating}
                    onChange={(event) => setPracticeMaxRating(event.target.value)}
                    onBlur={(event) => setPracticeMaxRating(normalizeToHundreds(event.target.value))}
                    placeholder="1600"
                  />
                </label>

                <label className="field-stack field-span-2">
                  <span>标签</span>
                  <input
                    className="auth-input"
                    value={practiceTags}
                    onChange={(event) => setPracticeTags(event.target.value)}
                    placeholder="例如 dp,graphs,math"
                  />
                </label>

                <label className="field-stack">
                  <span>常用标签</span>
                  <select
                    className="auth-input"
                    value={selectedPracticeTag}
                    onChange={(event) => setSelectedPracticeTag(event.target.value)}
                  >
                    {PRACTICE_TAG_OPTIONS.map((tag) => (
                      <option key={tag} value={tag}>
                        {tag}
                      </option>
                    ))}
                  </select>
                </label>

                <div className="filter-actions">
                  <button className="ghost-button" type="button" onClick={handleAddPresetTag}>
                    添加标签
                  </button>
                  <button
                    className="primary-button"
                    type="button"
                    disabled={isDrawingPractice}
                    onClick={handleDrawPractice}
                  >
                    {isDrawingPractice ? "抽题中..." : "开始抽题"}
                  </button>
                </div>
              </div>

                <EmbeddedProblemCard
                problem={practiceDraw?.problem}
                title="当前练习题"
                emptyText="完成条件设置后点击开始抽题。"
              />

              <div className="action-strip">
                <input
                  className="auth-input inline-input"
                  value={practiceSubmissionId}
                  onChange={(event) => setPracticeSubmissionId(event.target.value)}
                  placeholder="输入 Codeforces 提交 ID"
                />
                <button className="primary-button" type="button" disabled={actionBusy} onClick={handlePracticeCheck}>
                  提交校验
                </button>
              </div>
              </div>
            </section>

            <HistoryTimeline
              title="最近 30 条自主练习记录"
              items={practiceHistory}
              type="practice"
              emptyText={practiceHistoryLoading ? "正在加载记录..." : "暂时没有自主练习记录。"}
            />
          </div>
        )}

        {activeNav === "leaderboard" && <LeaderboardView auth={auth} />}

        {activeNav === "profile" && (
          <div className="portal-grid">
            <div className="portal-main-column">
              <section className="section-card profile-hero-card">
                <div className="profile-headline">
                  <div className="profile-avatar-wrap">
                    {user.avatarUrl ? (
                      <img className="profile-avatar-large" src={user.avatarUrl} alt="avatar" />
                    ) : (
                      <span className="profile-avatar-fallback">{getUserInitial(user)}</span>
                    )}
                  </div>
                  <div>
                    <p className="section-eyebrow">Profile</p>
                    <h2 style={{ color: ratingMeta.color }}>{user.username || "-"}</h2>
                    <p className="profile-rank-copy">{ratingMeta.label}</p>
                    <p className="profile-meta-copy">
                      UID {user.uid ?? "-"} · {formatOnlineText(user.online)} · 最近在线 {user.lastOnlineTimeIso || "-"}
                    </p>
                  </div>
                </div>

                <div className="profile-highlight-row">
                  <article className="profile-highlight">
                    <span>当前 rating</span>
                    <strong>{user.codeforcesRating ?? "-"}</strong>
                  </article>
                  <article className="profile-highlight">
                    <span>最大 rating</span>
                    <strong>{user.maxRating ?? "-"}</strong>
                  </article>
                  <article className="profile-highlight">
                    <span>总积分</span>
                    <strong>{formatScore(user.score)}</strong>
                  </article>
                </div>
              </section>

              <section className="section-card">
                <div className="section-heading section-heading-inline">
                  <div>
                    <p className="section-eyebrow">Profile Settings</p>
                    <h3>个人资料设置</h3>
                  </div>
                  {!profileEditMode ? (
                    <button className="ghost-button" type="button" onClick={enterProfileEditMode}>
                      编辑资料
                    </button>
                  ) : null}
                </div>

                {!profileEditMode ? (
                  <div className="profile-readonly-grid">
                    <article className="profile-data-card">
                      <span>用户名</span>
                      <strong>{user.username || "-"}</strong>
                    </article>
                    <article className="profile-data-card">
                      <span>邮箱</span>
                      <strong className="profile-email-value">{user.email || "-"}</strong>
                    </article>
                    <article className="profile-data-card">
                      <span>角色</span>
                      <strong>{user.role || "-"}</strong>
                    </article>
                  </div>
                ) : (
                  <>
                    <div className="filter-grid">
                      <label className="field-stack">
                        <span>用户名 / Codeforces handle</span>
                        <input
                          className="auth-input"
                          value={profileUsername}
                          onChange={(event) => setProfileUsername(event.target.value)}
                          placeholder="请输入用户名"
                        />
                        <small>{usernameHasSpace ? "用户名不能包含空格" : "修改后将同步刷新段位信息"}</small>
                      </label>

                      <label className="field-stack">
                        <span>邮箱</span>
                        <input
                          className="auth-input"
                          type="email"
                          value={profileEmail}
                          onChange={(event) => setProfileEmail(event.target.value)}
                          placeholder="可选"
                        />
                        <small>留空将清空邮箱</small>
                      </label>

                      <label className="field-stack field-span-2">
                        <span>新密码</span>
                        <input
                          className="auth-input"
                          type="password"
                          value={profilePassword}
                          onChange={(event) => setProfilePassword(event.target.value)}
                          placeholder="留空表示不修改密码"
                        />
                        <small>如果填写，长度至少 6 位</small>
                      </label>
                    </div>

                    <div className="profile-action-row">
                      <span className="profile-note">头像来自 Codeforces，本页面不支持手动上传。</span>
                      <button className="ghost-button" type="button" disabled={actionBusy} onClick={cancelProfileEditMode}>
                        取消
                      </button>
                      <button
                        className="primary-button"
                        type="button"
                        disabled={saveDisabled}
                        onClick={handleSaveProfile}
                      >
                        保存修改
                      </button>
                    </div>
                  </>
                )}
              </section>
            </div>

            <div className="portal-side-column">
              <section className="section-card side-info-card">
                <div className="section-heading">
                  <p className="section-eyebrow">Identity</p>
                  <h3>账号画像</h3>
                </div>
                <div className="identity-list">
                  <div className="identity-row">
                    <span>Handle</span>
                    <strong>{user.username || "-"}</strong>
                  </div>
                  <div className="identity-row">
                    <span>Role</span>
                    <strong>{user.role || "-"}</strong>
                  </div>
                  <div className="identity-row">
                    <span>邮箱</span>
                    <strong className="profile-email-value">{user.email || "-"}</strong>
                  </div>
                </div>
              </section>

              <section className="section-card side-info-card">
                <div className="section-heading">
                  <p className="section-eyebrow">Support</p>
                  <h3>使用提示</h3>
                </div>
                <div className="notice-list">
                  <article className="notice-item">
                    <strong>每日题打卡</strong>
                    <p>先在 Codeforces 提交，再把 submission ID 填回本平台。</p>
                  </article>
                  <article className="notice-item">
                    <strong>用户名修改</strong>
                    <p>用户名会作为 handle 使用，建议与 Codeforces 保持一致。</p>
                  </article>
                </div>
              </section>
            </div>
          </div>
        )}
      </section>
      )}
    </main>
  );
}
