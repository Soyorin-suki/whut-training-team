import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { getHomeOverview } from "../api/home";
import { getHeatmap } from "../api/user";
import { useAuth } from "../context/AuthContext";
import ProblemCard from "../components/ui/ProblemCard";
import Heatmap from "../components/ui/Heatmap";
import UserAvatar from "../components/ui/UserAvatar";
import { CardSkeleton } from "../components/ui/Skeleton";

function getTodayStr() {
  return new Date().toISOString().slice(0, 10);
}

export default function HomePage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [heatmapData, setHeatmapData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const [overview, heatmap] = await Promise.all([
          getHomeOverview(10),
          user?.id ? getHeatmap(user.id, 365) : Promise.resolve({ code: 200, data: [] }),
        ]);
        if (cancelled) return;
        setData(overview);
        if (heatmap?.code === 200) {
          setHeatmapData(heatmap.data || []);
        }
      } catch (e) {
        if (!cancelled) setError("加载首页数据失败");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [user?.id]);

  if (loading) {
    return (
      <div className="space-y-4">
        <h1 className="text-lg font-semibold">首页</h1>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <CardSkeleton />
          <CardSkeleton />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-error">{error}</p>
      </div>
    );
  }

  const { totalUsers, topUsers, todayProblem, todayPushProblem, dailySubmissionSummary } = data || {};

  // Determine today's display problems (backend now filters out redrawn slots)
  const todayProblems = Array.isArray(todayProblem) ? todayProblem : (todayProblem ? [todayProblem] : []);

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">首页</h1>

      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard label="活跃用户" value={totalUsers ?? 0} />
        <StatCard label="今日打卡" value={dailySubmissionSummary?.todayCheckedInUsers ?? 0} />
        <StatCard label="今日提交" value={dailySubmissionSummary?.todaySubmissions ?? 0} />
        <StatCard label="我的积分" value={user?.totalPoints ?? 0} />
      </div>

      {/* Heatmap */}
      <section>
        <h2 className="text-base font-semibold text-text-primary m-0 mb-2">打卡热力图</h2>
        <div className="bg-white border border-border rounded-ui p-4 overflow-x-auto">
          <Heatmap data={heatmapData} />
        </div>
      </section>

      {/* Main content */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Today's Problems */}
        <section>
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-base font-semibold text-text-primary m-0">今日题目</h2>
            <Link to="/daily" className="text-xs text-text-secondary hover:text-text-primary">
              查看详情 →
            </Link>
          </div>
          {todayProblems.length > 0 ? (
            <div className="space-y-2">
              {todayProblems.map((p, i) => (
                <ProblemCard key={p.problemKey || i} problem={p} />
              ))}
            </div>
          ) : (
            <div className="bg-white border border-border rounded-ui p-6 text-center text-sm text-text-secondary">
              今日题目尚未生成
            </div>
          )}
        </section>

        {/* Today's Push + Leaderboard preview */}
        <div className="space-y-4">
          {/* Today's Push */}
          <section>
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-base font-semibold text-text-primary m-0">今日推题</h2>
              <Link to="/push" className="text-xs text-text-secondary hover:text-text-primary">
                查看详情 →
              </Link>
            </div>
            {todayPushProblem ? (
              <div className="bg-white border border-border rounded-ui p-4">
                <h3 className="text-sm font-semibold text-text-primary m-0">
                  {todayPushProblem.title}
                </h3>
                {todayPushProblem.description ? (
                  <p className="text-xs text-text-secondary mt-1 m-0 line-clamp-2">
                    {todayPushProblem.description}
                  </p>
                ) : (
                  <p className="text-xs text-text-secondary mt-1 m-0">无描述</p>
                )}
                {todayPushProblem.link && (
                  <a
                    href={todayPushProblem.link}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-block mt-2 text-xs text-text-secondary hover:text-text-primary underline"
                  >
                    在外部打开 →
                  </a>
                )}
              </div>
            ) : (
              <div className="bg-white border border-border rounded-ui p-6 text-center text-sm text-text-secondary">
                今日无推题
              </div>
            )}
          </section>

          {/* Leaderboard Top 10 */}
          <section>
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-base font-semibold text-text-primary m-0">排行榜 Top 10</h2>
              <Link to="/leaderboard" className="text-xs text-text-secondary hover:text-text-primary">
                查看完整排行榜 →
              </Link>
            </div>
            {topUsers && topUsers.length > 0 ? (
              <div className="bg-white border border-border rounded-ui overflow-hidden">
                {topUsers.map((item, idx) => (
                  <div
                    key={item.userId ?? item.id}
                    className={`flex items-center gap-3 px-4 py-2.5 ${
                      idx > 0 ? "border-t border-border" : ""
                    }`}
                  >
                    <span className="text-xs text-text-secondary w-5 text-right font-mono">
                      {idx + 1}
                    </span>
                    <UserAvatar
                      user={{ username: item.username, avatarUrl: item.avatarUrl }}
                      size={24}
                    />
                    <span className="flex-1 text-sm text-text-primary truncate">
                      {item.username}
                    </span>
                    <span className="text-sm font-semibold text-text-primary">
                      {item.totalPoints ?? 0}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-white border border-border rounded-ui p-4 text-center text-sm text-text-secondary">
                暂无排行数据
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value, subtitle }) {
  return (
    <div className="bg-white border border-border rounded-ui px-4 py-3">
      <p className="text-xs text-text-secondary m-0">{label}</p>
      <p className="text-xl font-semibold text-text-primary m-0 mt-0.5">{value}</p>
      {subtitle && <p className="text-[11px] text-text-secondary m-0">{subtitle}</p>}
    </div>
  );
}
