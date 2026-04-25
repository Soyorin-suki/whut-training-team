import { useEffect, useState } from "react";
import { getUserInitial } from "../auth";
import { logout, updateMyProfile } from "../api/user";
import {
  checkInToday,
  checkPractice,
  drawPracticeProblem,
  getDailyHistory,
  getTodayProblem,
  regenerateTodayByAdmin
} from "../api/dailyProblem";

const NAV_ITEMS = [
  { key: "daily", label: "每日一题" },
  { key: "practice", label: "自主抽题" },
  { key: "profile", label: "个人信息" }
];

function parseTags(tags) {
  if (!tags) return [];
  return String(tags)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 6);
}

function getRatingMeta(rating) {
  if (rating === null || rating === undefined) return { label: "Unrated", color: "#666666" };
  if (rating < 1200) return { label: "Newbie", color: "#808080" };
  if (rating < 1400) return { label: "Pupil", color: "#008000" };
  if (rating < 1600) return { label: "Specialist", color: "#03a89e" };
  if (rating < 1900) return { label: "Expert", color: "#0000ff" };
  if (rating < 2100) return { label: "Candidate Master", color: "#aa00aa" };
  if (rating < 2400) return { label: "Master", color: "#ff8c00" };
  return { label: "Grandmaster", color: "#ff0000" };
}

function formatOnlineText(online) {
  if (online === null || online === undefined) return "-";
  return online ? "online" : "offline";
}

function ProblemCard({ problem }) {
  if (!problem) return null;
  return (
    <article className="problem-card">
      <div className="problem-header">
        <h3>
          {problem.contestId}
          {problem.problemIndex}. {problem.name}
        </h3>
        <span className="problem-rating">{problem.rating ?? "未定级"}</span>
      </div>
      <p className="problem-meta">
        日期：{problem.date} | 类型：{problem.type}
      </p>
      <div className="tag-list">
        {parseTags(problem.tags).map((tag) => (
          <span className="tag" key={tag}>
            {tag}
          </span>
        ))}
      </div>
      <a className="problem-link" href={problem.sourceUrl} target="_blank" rel="noreferrer">
        在 Codeforces 打开题目
      </a>
    </article>
  );
}

export default function MainView({ auth, onLogout, onNavigate, onUserUpdate }) {
  const [activeNav, setActiveNav] = useState("daily");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [profileEditMode, setProfileEditMode] = useState(false);

  const [todayData, setTodayData] = useState(null);
  const [history, setHistory] = useState([]);
  const [dailySubmissionId, setDailySubmissionId] = useState("");

  const [practiceMinRating, setPracticeMinRating] = useState(1200);
  const [practiceMaxRating, setPracticeMaxRating] = useState(1600);
  const [practiceDraw, setPracticeDraw] = useState(null);
  const [practiceSubmissionId, setPracticeSubmissionId] = useState("");

  const [profileUsername, setProfileUsername] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [profilePassword, setProfilePassword] = useState("");

  const user = auth?.user ?? null;
  const tokens = auth?.tokens ?? null;
  const isAdmin = user?.role === "ADMIN";
  const ratingMeta = getRatingMeta(user?.codeforcesRating);

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
  const hasProfileChanges = hasTextProfileChanges;
  const saveDisabled = loading || !hasProfileChanges || !normalizedProfileUsername || usernameHasSpace;

  useEffect(() => {
    setProfileUsername(user?.username || "");
    setProfileEmail(user?.email || "");
    setProfilePassword("");
    setProfileEditMode(false);
  }, [user?.id]);

  async function loadDailyPanel() {
    if (!tokens) return;
    setLoading(true);
    setMessage("");
    try {
      const [todayResp, historyResp] = await Promise.all([
        getTodayProblem(tokens),
        getDailyHistory(tokens, 14)
      ]);
      if (todayResp.code === 200) {
        setTodayData(todayResp.data);
      } else {
        setMessage(todayResp.message || "获取今日题失败");
      }
      if (historyResp.code === 200) {
        setHistory(historyResp.data || []);
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "加载每日题数据失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (user) {
      loadDailyPanel();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  async function handleLogout() {
    try {
      if (tokens?.accessToken && tokens?.refreshToken) {
        await logout(tokens);
      }
    } catch {
      // ignore
    } finally {
      onLogout?.();
    }
  }

  async function handleDailyCheckIn() {
    if (!dailySubmissionId.trim()) {
      setMessage("请输入提交 ID");
      return;
    }
    setLoading(true);
    setMessage("");
    try {
      const resp = await checkInToday(Number(dailySubmissionId), tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "打卡失败");
      } else {
        const result = resp.data || {};
        setMessage(
          `打卡成功：submissionId=${result.submissionId ?? "-"}，verdict=${result.verdict ?? "-"}，score=+${result.score ?? 0}`
        );
        setDailySubmissionId("");
        await loadDailyPanel();
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "打卡请求失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleRegenerate() {
    if (!isAdmin) return;
    setLoading(true);
    setMessage("");
    try {
      const resp = await regenerateTodayByAdmin(tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "重生成失败");
      } else {
        setMessage("已重生成今日题");
        await loadDailyPanel();
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "重生成请求失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleDrawPractice() {
    setLoading(true);
    setMessage("");
    try {
      const resp = await drawPracticeProblem(
        { minRating: Number(practiceMinRating), maxRating: Number(practiceMaxRating) },
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
      setLoading(false);
    }
  }

  async function handlePracticeCheck() {
    if (!practiceDraw?.drawId) {
      setMessage("请先抽题");
      return;
    }
    if (!practiceSubmissionId.trim()) {
      setMessage("请输入提交 ID");
      return;
    }
    setLoading(true);
    setMessage("");
    try {
      const resp = await checkPractice(practiceDraw.drawId, Number(practiceSubmissionId), tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "校验失败");
        return;
      }
      if (resp.data?.accepted) {
        setMessage("练习题通过（不计分）");
      } else {
        setMessage(`练习题未通过，判题结果=${resp.data?.verdict || "-"}`);
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "练习题校验请求失败");
    } finally {
      setLoading(false);
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
    if (!hasProfileChanges) {
      setMessage("没有需要保存的变更");
      return;
    }

    setLoading(true);
    setMessage("");
    try {
      const payload = {
        username: normalizedProfileUsername,
        email: normalizedProfileEmail || null,
        password: profilePassword || null
      };
      const resp = await updateMyProfile(payload, tokens);
      if (resp.code !== 200) {
        setMessage(resp.message || "个人信息更新失败");
        return;
      }
      onUserUpdate?.(resp.data);
      setMessage("个人信息已更新");
      setProfilePassword("");
      setProfileEditMode(false);
    } catch (error) {
      setMessage(error.response?.data?.message || "个人信息更新请求失败");
    } finally {
      setLoading(false);
    }
  }

  if (!user) {
    return (
      <main className="main-layout">
        <section className="content-area">
          <header className="content-topbar">
            <div className="content-header">
              <h1>WHUT Training</h1>
              <p>请先登录后使用系统功能。</p>
            </div>
            <div className="guest-actions">
              <button className="ghost-button" type="button" onClick={() => onNavigate?.("login")}>
                登录
              </button>
              <button className="primary-button" type="button" onClick={() => onNavigate?.("register")}>
                注册
              </button>
            </div>
          </header>
        </section>
      </main>
    );
  }

  return (
    <main className="main-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">WHUT Training</div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`sidebar-item ${activeNav === item.key ? "is-active" : ""}`}
              onClick={() => setActiveNav(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="content-area">
        <header className="content-topbar">
          <div className="content-header">
            <h1>{activeNav === "daily" ? "每日一题" : activeNav === "practice" ? "自主抽题" : "个人信息"}</h1>
            <p>
              {activeNav === "daily"
                ? "同一天全体用户使用同一道每日题。"
                : activeNav === "practice"
                  ? "自主抽题用于练习，不参与积分。"
                  : "参考 Codeforces 个人页样式，支持展示态与编辑态切换。"}
            </p>
          </div>
          <div className="guest-actions">
            {user.avatarUrl ? (
              <img className="avatar-image" src={user.avatarUrl} alt="avatar" />
            ) : (
              <span className="avatar-badge">{getUserInitial(user)}</span>
            )}
            <button className="ghost-button" type="button" onClick={handleLogout}>
              退出登录
            </button>
          </div>
        </header>

        {message && <p className="action-message">{message}</p>}
        {loading && <p className="action-message">处理中...</p>}

        {activeNav === "daily" && (
          <section className="panel-stack">
            <ProblemCard problem={todayData?.problem} />
            <div className="action-row">
              <input
                className="auth-input inline-input"
                value={dailySubmissionId}
                onChange={(event) => setDailySubmissionId(event.target.value)}
                placeholder="输入 Codeforces 提交 ID"
              />
              <button
                className="primary-button"
                type="button"
                disabled={todayData?.checkedIn}
                onClick={handleDailyCheckIn}
              >
                {todayData?.checkedIn ? "今日已打卡" : "提交打卡"}
              </button>
              {isAdmin && (
                <button className="ghost-button" type="button" onClick={handleRegenerate}>
                  管理员重生成
                </button>
              )}
            </div>
            <section className="history-panel">
              <h3>最近 14 天</h3>
              <div className="history-list">
                {history.map((item) => (
                  <article className="history-item" key={`${item.date}-${item.problemKey}`}>
                    <div>
                      <strong>{item.date}</strong>
                      <p>
                        {item.problemKey} | {item.name} | {item.rating ?? "未定级"}
                      </p>
                    </div>
                    <div>
                      {item.checkedIn ? (
                        <span className="history-ok">
                          已打卡（submissionId={item.submissionId ?? "-"}，verdict={item.verdict || "-"}，+{item.score ?? 0}）
                        </span>
                      ) : (
                        <span className="history-pending">未打卡</span>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </section>
        )}

        {activeNav === "practice" && (
          <section className="panel-stack">
            <div className="action-row">
              <input
                className="auth-input inline-input small"
                type="number"
                value={practiceMinRating}
                onChange={(event) => setPracticeMinRating(event.target.value)}
                placeholder="最小难度"
              />
              <input
                className="auth-input inline-input small"
                type="number"
                value={practiceMaxRating}
                onChange={(event) => setPracticeMaxRating(event.target.value)}
                placeholder="最大难度"
              />
              <button className="primary-button" type="button" onClick={handleDrawPractice}>
                抽题
              </button>
            </div>
            <ProblemCard problem={practiceDraw?.problem} />
            <div className="action-row">
              <input
                className="auth-input inline-input"
                value={practiceSubmissionId}
                onChange={(event) => setPracticeSubmissionId(event.target.value)}
                placeholder="输入练习题提交 ID"
              />
              <button className="primary-button" type="button" onClick={handlePracticeCheck}>
                提交校验（不计分）
              </button>
            </div>
          </section>
        )}

        {activeNav === "profile" && (
          <section className="cf-profile">
            <article className="cf-hero cf-hero-profile">
              <div className="cf-identity">
                {user.avatarUrl ? (
                  <img className="cf-hero-avatar" src={user.avatarUrl} alt="avatar" />
                ) : (
                  <span className="cf-hero-fallback">{getUserInitial(user)}</span>
                )}
                <div>
                  <h2 className="cf-handle" style={{ color: ratingMeta.color }}>
                    {user.username || "-"}
                  </h2>
                  <p className="cf-rank">{ratingMeta.label}</p>
                  <p className="cf-meta">
                    UID: {user.uid ?? "-"} · {formatOnlineText(user.online)} · 最近在线: {user.lastOnlineTimeIso || "-"}
                  </p>
                </div>
              </div>
              <div className="cf-rating-box">
                <span>current rating</span>
                <strong>{user.codeforcesRating ?? "-"}</strong>
                <small>max {user.maxRating ?? "-"}</small>
              </div>
            </article>

            <section className="cf-stat-grid">
              <article className="cf-stat">
                <span>角色</span>
                <strong>{user.role || "-"}</strong>
              </article>
              <article className="cf-stat">
                <span>邮箱</span>
                <strong>{user.email || "-"}</strong>
              </article>
              <article className="cf-stat">
                <span>Handle</span>
                <strong>{user.username || "-"}</strong>
              </article>
            </section>

            <section className="profile-settings profile-settings-cf">
              {!profileEditMode ? (
                <div className="profile-actions">
                  <button className="ghost-button" type="button" onClick={enterProfileEditMode}>
                    编辑资料
                  </button>
                </div>
              ) : (
                <>
                  <div className="profile-form-grid">
                    <label className="profile-field">
                      <span>用户名（Codeforces handle）</span>
                      <input
                        className="auth-input"
                        value={profileUsername}
                        onChange={(event) => setProfileUsername(event.target.value)}
                        placeholder="Codeforces 用户名"
                      />
                      <small>{usernameHasSpace ? "用户名不能包含空格" : "修改后会同步 Codeforces 等级信息"}</small>
                    </label>

                    <label className="profile-field">
                      <span>邮箱</span>
                      <input
                        className="auth-input"
                        type="email"
                        value={profileEmail}
                        onChange={(event) => setProfileEmail(event.target.value)}
                        placeholder="邮箱（可选）"
                      />
                      <small>留空可清除邮箱</small>
                    </label>

                    <label className="profile-field">
                      <span>新密码</span>
                      <input
                        className="auth-input"
                        type="password"
                        value={profilePassword}
                        onChange={(event) => setProfilePassword(event.target.value)}
                        placeholder="留空表示不修改密码"
                      />
                      <small>若填写，长度至少 6 位</small>
                    </label>

                  </div>

                  <div className="profile-actions">
                    <span className="profile-save-hint">
                      头像来源于 Codeforces，本页面不支持手动修改
                    </span>
                    <button className="ghost-button" type="button" disabled={loading} onClick={cancelProfileEditMode}>
                      取消
                    </button>
                    <button
                      className="primary-button cf-save-button"
                      type="button"
                      onClick={handleSaveProfile}
                      disabled={saveDisabled}
                    >
                      保存修改
                    </button>
                  </div>
                </>
              )}
            </section>
          </section>
        )}
      </section>
    </main>
  );
}
