import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const NAV_ITEMS = [
  { to: "/", label: "首页", icon: HomeIcon },
  { to: "/daily", label: "每日一题", icon: DailyIcon },
  { to: "/leaderboard", label: "排行榜", icon: LeaderboardIcon },
  { to: "/practice", label: "自主练习", icon: PracticeIcon },
  { to: "/push", label: "推题", icon: PushIcon },
  { to: "/profile", label: "个人中心", icon: ProfileIcon, requireAuth: true },
];

const ADMIN_ITEMS = [
  { to: "/admin/daily", label: "题管理", icon: AdminIcon },
  { to: "/admin/users", label: "用户管理", icon: UsersIcon },
  { to: "/admin/push", label: "推题审核", icon: ReviewIcon },
];

function HomeIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
      <polyline points="9 22 9 12 15 12 15 22" />
    </svg>
  );
}

function DailyIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  );
}

function LeaderboardIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5C7 4 6 9 6 9z" />
      <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5C17 4 18 9 18 9z" />
      <path d="M4 22h16" />
      <path d="M10 22V2h4v20" />
    </svg>
  );
}

function PracticeIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="16 18 22 12 16 6" />
      <polyline points="8 6 2 12 8 18" />
    </svg>
  );
}

function PushIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="22" y1="2" x2="11" y2="13" />
      <polygon points="22 2 15 22 11 13 2 9 22 2" />
    </svg>
  );
}

function ProfileIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function AdminIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  );
}

function UsersIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  );
}

function ReviewIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
      <polyline points="10 9 9 9 8 9" />
    </svg>
  );
}

export default function Sidebar() {
  const { isAuthenticated, isAdmin } = useAuth();
  const location = useLocation();
  const [expanded, setExpanded] = useState(false);

  const filteredItems = NAV_ITEMS.filter(
    (item) => !item.requireAuth || isAuthenticated
  );

  return (
    <aside
      className="flex-shrink-0 flex flex-col bg-bg-primary border-r border-border transition-all duration-200 ease-in-out overflow-hidden"
      style={{ width: expanded ? 200 : 56 }}
      onMouseEnter={() => setExpanded(true)}
      onMouseLeave={() => setExpanded(false)}
    >
      {/* Logo area */}
      <Link
        to="/"
        className="flex items-center h-14 px-3 border-b border-border flex-shrink-0 overflow-hidden"
      >
        <span className="flex-shrink-0 w-8 h-8 bg-text-primary text-white rounded-ui flex items-center justify-center text-sm font-bold">
          W
        </span>
        <span
          className={`ml-2.5 font-semibold text-base whitespace-nowrap transition-opacity duration-150 ${
            expanded ? "opacity-100" : "opacity-0"
          }`}
        >
          WHUT Training
        </span>
      </Link>

      {/* Nav items */}
      <nav className="flex-1 py-2 overflow-y-auto">
        {filteredItems.map((item) => {
          const active = location.pathname === item.to;
          return (
            <Link
              key={item.to}
              to={item.to}
              className={`flex items-center h-10 mx-1.5 px-3 rounded-ui text-nav transition-colors ${
                active
                  ? "bg-hover text-text-primary font-medium"
                  : "text-text-secondary hover:bg-[#f3f4f6]"
              }`}
            >
              <span
                className={`flex-shrink-0 flex items-center justify-center w-5 h-5 transition-transform ${
                  active ? "scale-110" : ""
                }`}
              >
                <item.icon />
              </span>
              <span
                className={`ml-2.5 whitespace-nowrap transition-opacity duration-150 ${
                  expanded ? "opacity-100" : "opacity-0"
                }`}
              >
                {item.label}
              </span>
            </Link>
          );
        })}

        {/* Admin section */}
        {isAdmin && (
          <>
            <div
              className={`mx-3 mt-3 mb-1 overflow-hidden transition-opacity duration-150 ${
                expanded ? "opacity-100" : "opacity-0"
              }`}
            >
              <span className="text-[11px] font-semibold uppercase text-text-secondary tracking-wide">
                管理
              </span>
            </div>
            {ADMIN_ITEMS.map((item) => {
              const active = location.pathname === item.to;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`flex items-center h-10 mx-1.5 px-3 rounded-ui text-nav transition-colors ${
                    active
                      ? "bg-hover text-text-primary font-medium"
                      : "text-text-secondary hover:bg-[#f3f4f6]"
                  }`}
                >
                  <span
                    className={`flex-shrink-0 flex items-center justify-center w-5 h-5 transition-transform ${
                      active ? "scale-110" : ""
                    }`}
                  >
                    <item.icon />
                  </span>
                  <span
                    className={`ml-2.5 whitespace-nowrap transition-opacity duration-150 ${
                      expanded ? "opacity-100" : "opacity-0"
                    }`}
                  >
                    {item.label}
                  </span>
                </Link>
              );
            })}
          </>
        )}
      </nav>
    </aside>
  );
}
