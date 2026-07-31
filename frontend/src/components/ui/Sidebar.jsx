import {
  CalendarDays,
  CalendarClock,
  ClipboardCheck,
  Code2,
  House,
  Send,
  Settings2,
  Trophy,
  UserRound,
  Users,
  X,
} from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const NAV_ITEMS = [
  { to: "/", label: "首页", icon: House },
  { to: "/daily", label: "每日一题", icon: CalendarDays },
  { to: "/contests", label: "近期比赛", icon: CalendarClock },
  { to: "/leaderboard", label: "排行榜", icon: Trophy },
  { to: "/practice", label: "自主练习", icon: Code2 },
  { to: "/push", label: "推题", icon: Send },
  { to: "/profile", label: "个人中心", icon: UserRound, requireAuth: true },
];

const ADMIN_ITEMS = [
  { to: "/admin/daily", label: "题管理", icon: Settings2 },
  { to: "/admin/users", label: "用户管理", icon: Users },
  { to: "/admin/push", label: "推题审核", icon: ClipboardCheck },
];

function NavLink({ item, active, collapsed, onNavigate }) {
  const Icon = item.icon;
  return (
    <Link
      to={item.to}
      onClick={onNavigate}
      className={`sidebar-link ${active ? "is-active" : ""}`}
      aria-label={item.label}
      title={collapsed ? item.label : undefined}
    >
      <Icon size={19} strokeWidth={1.8} />
      <span className="sidebar-label">{item.label}</span>
      {active && <span className="sidebar-active-dot" aria-hidden="true" />}
    </Link>
  );
}

export default function Sidebar({
  collapsed,
  mobileOpen,
  onClose,
  onHoverStart,
  onHoverEnd,
}) {
  const { isAuthenticated, isAdmin } = useAuth();
  const location = useLocation();
  const filteredItems = NAV_ITEMS.filter(
    (item) => !item.requireAuth || isAuthenticated
  );

  return (
    <aside
      className={`app-sidebar ${collapsed ? "is-collapsed" : ""} ${
        mobileOpen ? "is-mobile-open" : ""
      }`}
      onMouseEnter={onHoverStart}
      onMouseLeave={onHoverEnd}
    >
      <div className="sidebar-brand">
        <Link to="/" className="sidebar-brand-link" onClick={onClose}>
          <span className="sidebar-brand-mark">
            <img src="/whut-acm-logo.png" alt="" aria-hidden="true" />
          </span>
          <span className="sidebar-brand-copy">
            <strong>WHUT-ACM</strong>
            <small>TRAINING LAB</small>
          </span>
        </Link>
        <button className="sidebar-mobile-close" onClick={onClose} aria-label="关闭菜单">
          <X size={19} />
        </button>
      </div>

      <nav className="sidebar-nav" aria-label="主导航">
        <div className="sidebar-section-label">WORKSPACE</div>
        {filteredItems.map((item) => (
          <NavLink
            key={item.to}
            item={item}
            active={location.pathname === item.to}
            collapsed={collapsed}
            onNavigate={onClose}
          />
        ))}

        {isAdmin && (
          <div className="sidebar-admin">
            <div className="sidebar-section-label">ADMIN</div>
            {ADMIN_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                item={item}
                active={location.pathname === item.to}
                collapsed={collapsed}
                onNavigate={onClose}
              />
            ))}
          </div>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-status">
          <span className="status-pulse" />
          <span className="sidebar-label">SYSTEM ONLINE</span>
        </div>
      </div>
    </aside>
  );
}
