import { useMemo, useState } from "react";
import { getUserInitial } from "../auth";
import { logout } from "../api/user";

const NAV_ITEMS = [
  { key: "overview", label: "总览" },
  { key: "courses", label: "课程管理" },
  { key: "members", label: "团队成员" },
  { key: "tasks", label: "任务看板" },
  { key: "settings", label: "系统设置" }
];

const MOCK_CONTENT = {
  overview: {
    title: "项目总览",
    desc: "这里展示训练团队的整体运行情况，当前使用的是演示数据。",
    cards: [
      { name: "今日活跃用户", value: "128" },
      { name: "本周新增任务", value: "34" },
      { name: "待审核申请", value: "7" }
    ]
  },
  courses: {
    title: "课程管理",
    desc: "查看课程排期、授课状态和阶段进展。",
    cards: [
      { name: "Java Web 入门", value: "进行中" },
      { name: "前端工程实践", value: "未开始" },
      { name: "数据库专项训练", value: "进行中" }
    ]
  },
  members: {
    title: "团队成员",
    desc: "展示核心成员和当前承担角色。",
    cards: [
      { name: "张三", value: "后端开发" },
      { name: "李四", value: "前端开发" },
      { name: "王五", value: "测试支持" }
    ]
  },
  tasks: {
    title: "任务看板",
    desc: "跟踪任务的流转状态和处理节奏。",
    cards: [
      { name: "待处理", value: "12" },
      { name: "进行中", value: "9" },
      { name: "已完成", value: "56" }
    ]
  },
  settings: {
    title: "系统设置",
    desc: "管理通知、同步和日志等基础配置。",
    cards: [
      { name: "通知开关", value: "已开启" },
      { name: "数据同步", value: "每 30 分钟" },
      { name: "日志保留", value: "30 天" }
    ]
  }
};

export default function MainView({ auth, onLogout, onNavigate }) {
  const [activeNav, setActiveNav] = useState("overview");
  const [showProfile, setShowProfile] = useState(false);
  const panel = MOCK_CONTENT[activeNav];
  const user = auth?.user ?? null;

  const profileItems = useMemo(
    () =>
      user
        ? [
            { label: "用户名", value: user.username || "-" },
            { label: "邮箱", value: user.email || "-" },
            { label: "角色", value: user.role || "-" },
            { label: "用户 ID", value: user.id ?? "-" }
          ]
        : [],
    [user]
  );

  async function handleLogout() {
    try {
      if (auth?.tokens?.accessToken && auth?.tokens?.refreshToken) {
        await logout(auth.tokens);
      }
    } catch {
      // Frontend state should still clear even if backend logout fails.
    } finally {
      setShowProfile(false);
      onLogout?.();
    }
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
            <h1>{panel.title}</h1>
            <p>{panel.desc}</p>
          </div>

          {!user ? (
            <div className="guest-actions">
              <button className="ghost-button" type="button" onClick={() => onNavigate?.("login")}>
                登录
              </button>
              <button
                className="primary-button"
                type="button"
                onClick={() => onNavigate?.("register")}
              >
                注册
              </button>
            </div>
          ) : (
            <div className="profile-anchor">
              <button
                className="avatar-button"
                type="button"
                onClick={() => setShowProfile((prev) => !prev)}
              >
                <span className="avatar-badge">{getUserInitial(user)}</span>
                <span className="avatar-name">{user.username || "已登录用户"}</span>
              </button>

              {showProfile && (
                <div className="profile-card">
                  <div className="profile-summary">
                    <span className="profile-avatar-large">{getUserInitial(user)}</span>
                    <div>
                      <strong>{user.username || "未命名用户"}</strong>
                      <p>{user.email || "未设置邮箱"}</p>
                    </div>
                  </div>

                  <div className="profile-list">
                    {profileItems.map((item) => (
                      <div className="profile-row" key={item.label}>
                        <span>{item.label}</span>
                        <strong>{item.value}</strong>
                      </div>
                    ))}
                  </div>

                  <button className="danger-button" type="button" onClick={handleLogout}>
                    退出登录
                  </button>
                </div>
              )}
            </div>
          )}
        </header>

        <div className="content-grid">
          {panel.cards.map((card) => (
            <article className="content-card" key={card.name}>
              <h2>{card.name}</h2>
              <strong>{card.value}</strong>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
