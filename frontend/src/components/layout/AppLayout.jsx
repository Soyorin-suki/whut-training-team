import { useEffect } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "../ui/Sidebar";
import TopBar from "../ui/TopBar";
import DevPanel from "../ui/DevPanel";
import { useAuth } from "../../context/AuthContext";
import { checkDevStatus } from "../../api/dev";

const IS_VITE_DEV = import.meta.env.DEV;

export default function AppLayout() {
  const { isDevBackend, setDevMode } = useAuth();

  // 仅 Vite dev 模式下尝试探测后端 profile，避免 prod 产生无意义的 404 请求
  useEffect(() => {
    if (!IS_VITE_DEV) return;
    let cancelled = false;
    checkDevStatus()
      .then((resp) => {
        if (!cancelled && resp?.code === 200) {
          setDevMode(true);
        }
      })
      .catch(() => {
        // prod 后端无 /api/dev/status → 404，静默忽略
      });
    return () => { cancelled = true; };
  }, [setDevMode]);

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <TopBar />
        <main className="flex-1 overflow-y-auto p-page" style={{ maxWidth: 1400 }}>
          <Outlet />
          {isDevBackend && (
            <div className="mt-5">
              <DevPanel />
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
