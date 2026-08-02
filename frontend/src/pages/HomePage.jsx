import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getHomeOverview } from "../api/home";
import { getHeatmap } from "../api/user";
import { useAuth } from "../context/AuthContext";
import ProblemCard from "../components/ui/ProblemCard";
import Heatmap from "../components/ui/Heatmap";
import UserAvatar from "../components/ui/UserAvatar";
import { CardSkeleton } from "../components/ui/Skeleton";
import { Activity, ArrowUpRight, Flame, LoaderCircle, Send, Trophy } from "lucide-react";
import CodeforcesOverview from "../components/profile/CodeforcesOverview";

function getTodayStr() {
  return new Date().toISOString().slice(0, 10);
}

export default function HomePage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [heatmapData, setHeatmapData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [heatmapLoading, setHeatmapLoading] = useState(true);
  const [error, setError] = useState("");
  const [heatmapError, setHeatmapError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setHeatmapLoading(Boolean(user?.id));
    setError("");
    setHeatmapError("");

    getHomeOverview(10)
      .then((overview) => {
        if (!cancelled) setData(overview);
      })
      .catch(() => {
        if (!cancelled) setError("加载首页数据失败");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    if (user?.id) {
      getHeatmap(user.id, 365)
        .then((heatmap) => {
          if (!cancelled && heatmap?.code === 200) {
            setHeatmapData(heatmap.data || []);
          }
        })
        .catch(() => {
          if (!cancelled) setHeatmapError("热力图暂时无法加载");
        })
        .finally(() => {
          if (!cancelled) setHeatmapLoading(false);
        });
    } else {
      setHeatmapData([]);
      setHeatmapLoading(false);
    }

    return () => { cancelled = true; };
  }, [user?.id]);

  useEffect(() => {
    if (!data?.problemPoolInitializing) return undefined;

    let requesting = false;
    const timer = window.setInterval(async () => {
      if (requesting) return;
      requesting = true;
      try {
        const overview = await getHomeOverview(10);
        setData(overview);
        setError("");
      } catch {
        // Keep the current overview visible and retry while the pool is initializing.
      } finally {
        requesting = false;
      }
    }, 3000);

    return () => window.clearInterval(timer);
  }, [data?.problemPoolInitializing]);

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
      <header className="dashboard-hero">
        <div>
          <p className="dashboard-eyebrow">/ DASHBOARD · {getTodayStr()}</p>
          <h1>今天，也向前一步。</h1>
          <p>专注训练，记录每一次提交与成长。</p>
        </div>
        <div className="dashboard-hero-aside">
          <div className="dashboard-user-chip">
            <UserAvatar user={user} size={56} />
            <span>
              <strong>{user?.displayName || user?.username || "训练者"}</strong>
              <small>@{user?.username || "guest"}</small>
            </span>
          </div>
          <Link to="/daily" className="dashboard-hero-action">
            开始今日训练 <ArrowUpRight size={17} />
          </Link>
        </div>
        <span className="dashboard-shape dashboard-shape-circle" aria-hidden="true" />
        <span className="dashboard-shape dashboard-shape-square" aria-hidden="true" />
      </header>

      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard icon={Activity} index="01" label="活跃用户" value={totalUsers ?? 0} />
        <StatCard icon={Flame} index="02" label="今日打卡" value={dailySubmissionSummary?.todayCheckedInUsers ?? 0} />
        <StatCard icon={Send} index="03" label="今日提交" value={dailySubmissionSummary?.todaySubmissions ?? 0} />
        <StatCard icon={Trophy} index="04" label="我的积分" value={user?.totalPoints ?? 0} />
      </div>

      <CodeforcesOverview
        userId={user?.id}
        handle={user?.codeforcesHandle}
        compact
      />

      {/* Heatmap */}
      <section className="render-lazy">
        <h2 className="text-base font-semibold text-text-primary m-0 mb-2">打卡热力图</h2>
        {heatmapLoading ? (
          <CardSkeleton />
        ) : heatmapError ? (
          <div className="bg-white border border-border rounded-ui p-6 text-sm text-text-secondary text-center">
            {heatmapError}
          </div>
        ) : (
          <div className="bg-white border border-border rounded-ui p-5 overflow-hidden">
            <Heatmap data={heatmapData} />
          </div>
        )}
      </section>

      {/* Main content */}
      <div className="render-lazy grid grid-cols-1 lg:grid-cols-2 gap-4">
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
          ) : data?.problemPoolInitializing ? (
            <div className="bg-white border border-border rounded-ui p-6 text-center text-sm text-text-secondary">
              <LoaderCircle className="inline-block mr-2 animate-spin" size={16} />
              首次启动正在初始化 Codeforces 题库，完成后会自动显示今日题目
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
                  <Link
                    key={item.userId ?? item.id}
                    to={`/members/${item.userId ?? item.id}`}
                    className={`flex items-center gap-3 px-4 py-2.5 ${
                      idx > 0 ? "border-t border-border" : ""
                    } no-underline hover:bg-bg-secondary transition-colors`}
                  >
                    <span className="text-xs text-text-secondary w-5 text-right font-mono">
                      {idx + 1}
                    </span>
                    <UserAvatar
                      user={{
                        username: item.displayName || item.username,
                        avatarUrl: item.avatarUrl,
                      }}
                      size={24}
                    />
                    <span className="flex-1 text-sm text-text-primary truncate">
                      {item.displayName || item.username}
                    </span>
                    <span className="text-sm font-semibold text-text-primary">
                      {item.totalPoints ?? 0}
                    </span>
                  </Link>
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

function StatCard({ icon: Icon, index, label, value, subtitle }) {
  return (
    <div className="stat-card">
      <div className="stat-card-top">
        <span>{index}</span>
        <Icon size={17} strokeWidth={1.7} />
      </div>
      <p>{label}</p>
      <strong>{value}</strong>
      {subtitle && <small>{subtitle}</small>}
    </div>
  );
}
