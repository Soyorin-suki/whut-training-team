import { useEffect, useState } from "react";
import { buildAuthFromLogin } from "../auth";
import { login, registerUser } from "../api/user";

const LOGIN = "login";
const REGISTER = "register";

const FEATURE_CARDS = [
  {
    id: "daily",
    label: "Daily",
    title: "每日一题",
    description: "每天统一题面、统一打卡，训练节奏更稳定。"
  },
  {
    id: "practice",
    label: "Practice",
    title: "自主练习",
    description: "按难度和标签抽题，专门补齐薄弱专题。"
  },
  {
    id: "rank",
    label: "Rank",
    title: "积分榜",
    description: "训练结果实时沉淀成排行，方便追踪成长。"
  }
];

const LANDING_NOTES = [
  "接入 Codeforces 账号信息，自动同步 handle 与 rating。",
  "每日训练与自主练习分离，既能完成统一打卡，也能定向补题。",
  "个人主页展示积分、段位、最近在线和训练状态。"
];

function LandingCard({ item }) {
  return (
    <article className="landing-feature-card" key={item.id}>
      <span className="landing-feature-kicker">{item.label}</span>
      <h3>{item.title}</h3>
      <p>{item.description}</p>
    </article>
  );
}

export default function HomeView({ initialPage = LOGIN, onAuthSuccess, onNavigate }) {
  const [page, setPage] = useState(initialPage);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [registerForm, setRegisterForm] = useState({
    username: "",
    password: "",
    confirmPassword: "",
    email: ""
  });

  useEffect(() => {
    setPage(initialPage);
    setMessage("");
  }, [initialPage]);

  async function onLogin(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");

    try {
      const resp = await login({
        username: loginForm.username.trim(),
        password: loginForm.password
      });
      if (resp.code !== 200) {
        setMessage(resp.message || "登录失败");
        return;
      }

      const auth = buildAuthFromLogin(resp.data);
      if (!auth) {
        setMessage("登录成功，但凭证信息不完整");
        return;
      }
      onAuthSuccess?.(auth);
    } catch (error) {
      setMessage(error.response?.data?.message || "登录请求失败");
    } finally {
      setBusy(false);
    }
  }

  async function onRegister(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");

    const username = registerForm.username.trim();
    const password = registerForm.password;
    const confirmPassword = registerForm.confirmPassword;
    const email = registerForm.email.trim() || `${username}@whut.local`;

    if (password !== confirmPassword) {
      setBusy(false);
      setMessage("两次输入的密码不一致");
      return;
    }

    try {
      const resp = await registerUser({ username, password, email });
      if (resp.code !== 200) {
        setMessage(resp.message || "注册失败");
        return;
      }

      setMessage("注册成功，请使用新账号登录");
      setPage(LOGIN);
      onNavigate?.(LOGIN);
      setLoginForm({ username, password: "" });
      setRegisterForm({ username: "", password: "", confirmPassword: "", email: "" });
    } catch (error) {
      setMessage(error.response?.data?.message || "注册请求失败");
    } finally {
      setBusy(false);
    }
  }

  function switchPage(nextPage) {
    setMessage("");
    setPage(nextPage);
    onNavigate?.(nextPage);
  }

  return (
    <main className="landing-shell">
      <header className="site-header landing-header">
        <div className="site-brand">
          <span className="site-brand-mark">WHUT</span>
          <div>
            <strong>WHUT Training Portal</strong>
            <span>Algorithm training, daily check-in, and rank tracking</span>
          </div>
        </div>

        <nav className="site-nav">
          <button className="site-nav-link is-active" type="button">
            门户首页
          </button>
          <button className="site-nav-link" type="button" onClick={() => switchPage(LOGIN)}>
            进入训练
          </button>
          <button className="site-nav-link" type="button" onClick={() => switchPage(REGISTER)}>
            新人注册
          </button>
        </nav>

        <div className="site-actions">
          <button
            className={`ghost-button ${page === LOGIN ? "is-current" : ""}`}
            type="button"
            onClick={() => switchPage(LOGIN)}
          >
            登录
          </button>
          <button
            className={`primary-button ${page === REGISTER ? "is-current" : ""}`}
            type="button"
            onClick={() => switchPage(REGISTER)}
          >
            注册
          </button>
        </div>
      </header>

      <section className="landing-hero">
        <div className="landing-copy">
          <p className="section-eyebrow">Wuhan University of Technology</p>
          <h1>把训练平台做成一个真正可用的校园算法门户。</h1>
          <p className="landing-lead">
            参考校 ACM 门户的结构，把登录、训练、积分榜和个人画像收拢到同一套信息架构里，
            让训练入口、数据反馈和近期记录都集中在一个首页完成。
          </p>

          <div className="landing-feature-grid">
            {FEATURE_CARDS.map((item) => (
              <LandingCard item={item} key={item.id} />
            ))}
          </div>

          <section className="landing-note-panel">
            <div className="section-heading">
              <p className="section-eyebrow">Portal Notes</p>
              <h2>当前平台包含的核心能力</h2>
            </div>
            <ul className="landing-note-list">
              {LANDING_NOTES.map((note) => (
                <li key={note}>{note}</li>
              ))}
            </ul>
          </section>
        </div>

        <section className="auth-portal-card">
          <div className="auth-portal-head">
            <p className="section-eyebrow">Account</p>
            <h2>{page === LOGIN ? "登录训练门户" : "创建训练账号"}</h2>
            <p>
              {page === LOGIN
                ? "使用训练账号进入每日打卡、自主练习和排行榜。"
                : "注册后可同步个人训练积分与 Codeforces 信息。"}
            </p>
          </div>

          <div className="auth-tab-row">
            <button
              className={`auth-tab ${page === LOGIN ? "is-active" : ""}`}
              type="button"
              onClick={() => switchPage(LOGIN)}
            >
              登录
            </button>
            <button
              className={`auth-tab ${page === REGISTER ? "is-active" : ""}`}
              type="button"
              onClick={() => switchPage(REGISTER)}
            >
              注册
            </button>
          </div>

          {message && <p className="system-message">{message}</p>}

          {page === LOGIN ? (
            <form className="auth-form" onSubmit={onLogin}>
              <label className="field-stack">
                <span>用户名 / Codeforces handle</span>
                <input
                  className="auth-input"
                  value={loginForm.username}
                  onChange={(event) =>
                    setLoginForm((prev) => ({ ...prev, username: event.target.value }))
                  }
                  placeholder="请输入用户名"
                  required
                />
              </label>

              <label className="field-stack">
                <span>密码</span>
                <input
                  className="auth-input"
                  value={loginForm.password}
                  onChange={(event) =>
                    setLoginForm((prev) => ({ ...prev, password: event.target.value }))
                  }
                  type="password"
                  placeholder="请输入密码"
                  required
                />
              </label>

              <button className="primary-button auth-submit" type="submit" disabled={busy}>
                {busy ? "登录中..." : "登录"}
              </button>
            </form>
          ) : (
            <form className="auth-form" onSubmit={onRegister}>
              <label className="field-stack">
                <span>用户名 / Codeforces handle</span>
                <input
                  className="auth-input"
                  value={registerForm.username}
                  onChange={(event) =>
                    setRegisterForm((prev) => ({ ...prev, username: event.target.value }))
                  }
                  placeholder="请输入用户名"
                  required
                />
              </label>

              <label className="field-stack">
                <span>密码</span>
                <input
                  className="auth-input"
                  value={registerForm.password}
                  onChange={(event) =>
                    setRegisterForm((prev) => ({ ...prev, password: event.target.value }))
                  }
                  type="password"
                  placeholder="至少 6 位"
                  required
                />
              </label>

              <label className="field-stack">
                <span>确认密码</span>
                <input
                  className="auth-input"
                  value={registerForm.confirmPassword}
                  onChange={(event) =>
                    setRegisterForm((prev) => ({ ...prev, confirmPassword: event.target.value }))
                  }
                  type="password"
                  placeholder="请再次输入密码"
                  required
                />
              </label>

              <label className="field-stack">
                <span>邮箱</span>
                <input
                  className="auth-input"
                  value={registerForm.email}
                  onChange={(event) =>
                    setRegisterForm((prev) => ({ ...prev, email: event.target.value }))
                  }
                  type="email"
                  placeholder="可选"
                />
              </label>

              <button className="primary-button auth-submit" type="submit" disabled={busy}>
                {busy ? "提交中..." : "注册"}
              </button>
            </form>
          )}
        </section>
      </section>

      <section className="landing-strip">
        <article className="landing-strip-card">
          <span>01</span>
          <strong>训练信息集中到首页</strong>
          <p>每日题、练习记录、榜单和个人状态在同一入口内完成浏览。</p>
        </article>
        <article className="landing-strip-card">
          <span>02</span>
          <strong>校园门户化视觉</strong>
          <p>保留训练平台的功能密度，但改为学院站点式的层级和节奏。</p>
        </article>
        <article className="landing-strip-card">
          <span>03</span>
          <strong>兼顾桌面端与移动端</strong>
          <p>导航、卡片和表单统一自适应，不再依赖侧边栏结构。</p>
        </article>
      </section>
    </main>
  );
}
