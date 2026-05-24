import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import AppLayout from "./components/layout/AppLayout";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import HomePage from "./pages/HomePage";
import DailyProblemPage from "./pages/DailyProblemPage";
import LeaderboardPage from "./pages/LeaderboardPage";
import PracticePage from "./pages/PracticePage";
import PushPage from "./pages/PushPage";
import ProfilePage from "./pages/ProfilePage";
import AdminDailyPage from "./pages/admin/AdminDailyPage";
import AdminUsersPage from "./pages/admin/AdminUsersPage";
import AdminPushPage from "./pages/admin/AdminPushPage";
import NotFoundPage from "./pages/NotFoundPage";

function AuthGuard({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-text-secondary">加载中...</p>
      </div>
    );
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function AdminGuard({ children }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-text-secondary">权限不足，需要管理员角色</p>
      </div>
    );
  }
  return children;
}

function GuestGuard({ children }) {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return children;
}

export default function App() {
  return (
    <Routes>
      {/* Public / guest routes */}
      <Route path="/login" element={<GuestGuard><LoginPage /></GuestGuard>} />
      <Route path="/register" element={<GuestGuard><RegisterPage /></GuestGuard>} />

      {/* Authenticated routes */}
      <Route element={<AuthGuard><AppLayout /></AuthGuard>}>
        <Route path="/" element={<HomePage />} />
        <Route path="/daily" element={<DailyProblemPage />} />
        <Route path="/leaderboard" element={<LeaderboardPage />} />
        <Route path="/practice" element={<PracticePage />} />
        <Route path="/push" element={<PushPage />} />
        <Route path="/profile" element={<ProfilePage />} />

        {/* Admin routes */}
        <Route path="/admin/daily" element={<AdminGuard><AdminDailyPage /></AdminGuard>} />
        <Route path="/admin/users" element={<AdminGuard><AdminUsersPage /></AdminGuard>} />
        <Route path="/admin/push" element={<AdminGuard><AdminPushPage /></AdminGuard>} />
      </Route>

      {/* 404 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
