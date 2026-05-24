import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import UserAvatar from "./UserAvatar";
import * as Dropdown from "@radix-ui/react-dropdown-menu";

export default function TopBar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="flex items-center justify-between h-14 px-6 bg-bg-primary border-b border-border flex-shrink-0">
      <Link to="/" className="text-base font-semibold text-text-primary">
        WHUT Training
      </Link>

      <div className="flex items-center gap-3">
        {isAuthenticated && user ? (
          <Dropdown.Root>
            <Dropdown.Trigger asChild>
              <button className="flex items-center gap-2 px-2 py-1 rounded-ui hover:bg-bg-secondary transition-colors border-0 bg-transparent cursor-pointer">
                <UserAvatar user={user} size={28} />
                <span className="text-nav text-text-primary max-w-[140px] truncate">
                  {user.displayName || user.username || ""}
                </span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
            </Dropdown.Trigger>
            <Dropdown.Portal>
              <Dropdown.Content
                className="min-w-[160px] bg-white rounded-ui shadow-md border border-border p-1 z-50"
                sideOffset={4}
                align="end"
              >
                <Dropdown.Item
                  className="flex items-center px-3 py-2 text-nav text-text-primary rounded cursor-pointer hover:bg-bg-secondary outline-none"
                  onClick={() => navigate("/profile")}
                >
                  个人资料
                </Dropdown.Item>
                <Dropdown.Separator className="h-px bg-border my-1" />
                <Dropdown.Item
                  className="flex items-center px-3 py-2 text-nav text-error rounded cursor-pointer hover:bg-bg-secondary outline-none"
                  onClick={() => logout()}
                >
                  退出登录
                </Dropdown.Item>
              </Dropdown.Content>
            </Dropdown.Portal>
          </Dropdown.Root>
        ) : (
          <div className="flex items-center gap-2">
            <button
              onClick={() => navigate("/login")}
              className="px-3 py-1.5 text-nav text-text-secondary hover:text-text-primary border-0 bg-transparent cursor-pointer rounded-ui"
            >
              登录
            </button>
            <button
              onClick={() => navigate("/register")}
              className="px-3 py-1.5 text-nav text-white bg-text-primary hover:bg-[#1b1f23] border-0 rounded-ui cursor-pointer font-medium"
            >
              注册
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
