import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { getUserById, updateMyProfile, getHeatmap } from "../api/user";
import { getRatingMeta } from "../utils/cf";
import UserAvatar from "../components/ui/UserAvatar";
import Heatmap from "../components/ui/Heatmap";
import { CardSkeleton } from "../components/ui/Skeleton";
import EmptyState from "../components/ui/EmptyState";

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [heatmapData, setHeatmapData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  // Edit mode
  const [editMode, setEditMode] = useState(false);
  const [editUsername, setEditUsername] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [editDisplayName, setEditDisplayName] = useState("");
  const [editBio, setEditBio] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user?.id) return;
    let cancelled = false;
    async function load() {
      try {
        const [userResp, heatmapResp] = await Promise.all([
          getUserById(user.id),
          getHeatmap(user.id, 365),
        ]);
        if (!cancelled) {
          if (userResp.code === 200) setProfile(userResp.data);
          if (heatmapResp.code === 200) setHeatmapData(heatmapResp.data || []);
        }
      } catch {
        // ignore
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [user?.id]);

  function enterEditMode() {
    setEditUsername(profile?.username || "");
    setEditEmail(profile?.email || "");
    setEditPassword("");
    setEditDisplayName(profile?.displayName || "");
    setEditBio(profile?.bio || "");
    setEditMode(true);
    setMessage("");
  }

  function cancelEdit() {
    setEditMode(false);
    setMessage("");
  }

  async function handleSave() {
    setSaving(true);
    setMessage("");
    try {
      const payload = {};
      if (editUsername.trim()) payload.username = editUsername.trim();
      if (editEmail.trim()) payload.email = editEmail.trim();
      else payload.email = null;
      if (editPassword.trim()) payload.password = editPassword;
      if (editDisplayName.trim()) payload.displayName = editDisplayName.trim();
      if (editBio.trim()) payload.bio = editBio.trim();

      const resp = await updateMyProfile(user.id, payload);
      if (resp.code === 200) {
        updateUser(resp.data);
        setProfile(resp.data);
        setEditMode(false);
        setMessage("资料已更新");
      } else {
        setMessage(resp.message || "更新失败");
      }
    } catch {
      setMessage("更新请求失败");
    } finally {
      setSaving(false);
    }
  }

  const currentProfile = profile || user;
  const ratingMeta = getRatingMeta(currentProfile?.codeforcesRating);

  if (loading) {
    return (
      <div className="space-y-4">
        <h1 className="text-lg font-semibold">个人中心</h1>
        <CardSkeleton />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">个人中心</h1>

      {message && (
        <p
          className={`text-sm rounded-ui px-3 py-2 m-0 ${
            message.includes("成功") || message.includes("已更新")
              ? "bg-[#f0fff0] text-success"
              : "text-text-secondary bg-bg-secondary"
          }`}
        >
          {message}
        </p>
      )}

      {/* CF-style profile card */}
      <div className="bg-white border border-border rounded-ui overflow-hidden">
        <div className="flex items-start gap-4 p-5">
          <UserAvatar user={currentProfile} size={64} />
          <div className="flex-1 min-w-0">
            <h2 className="text-lg font-bold m-0" style={{ color: ratingMeta.color }}>
              {currentProfile?.username || "-"}
            </h2>
            <p className="text-sm text-text-secondary m-0">{ratingMeta.label}</p>
            {currentProfile?.displayName && (
              <p className="text-sm text-text-primary mt-1 m-0">{currentProfile.displayName}</p>
            )}
            {currentProfile?.bio && (
              <p className="text-xs text-text-secondary mt-0.5 m-0">{currentProfile.bio}</p>
            )}
            <div className="flex gap-4 mt-2 text-xs text-text-secondary">
              <span>UID: {currentProfile?.uid ?? "-"}</span>
              <span>积分: {currentProfile?.totalPoints ?? 0}</span>
              <span>角色: {currentProfile?.role || "-"}</span>
            </div>
          </div>
          <div className="text-right flex-shrink-0">
            <p className="text-xs text-text-secondary m-0">current rating</p>
            <p className="text-xl font-bold m-0" style={{ color: ratingMeta.color }}>
              {currentProfile?.codeforcesRating ?? "-"}
            </p>
            <p className="text-xs text-text-secondary m-0">
              max {currentProfile?.maxRating ?? "-"}
            </p>
          </div>
        </div>
        <div className="border-t border-border px-5 py-2.5 flex justify-end">
          {!editMode ? (
            <button
              className="px-3 py-1 text-sm border border-border rounded-ui bg-white text-text-primary hover:bg-bg-secondary cursor-pointer"
              onClick={enterEditMode}
            >
              编辑资料
            </button>
          ) : (
            <div className="flex gap-2">
              <button
                className="px-3 py-1 text-sm border border-border rounded-ui bg-white text-text-primary hover:bg-bg-secondary cursor-pointer"
                onClick={cancelEdit}
                disabled={saving}
              >
                取消
              </button>
              <button
                className="px-3 py-1 text-sm text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer disabled:opacity-50"
                onClick={handleSave}
                disabled={saving}
              >
                {saving ? "保存中..." : "保存"}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Edit form */}
      {editMode && (
        <div className="bg-white border border-border rounded-ui p-4 space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs text-text-secondary">用户名 (CF handle)</span>
              <input
                className="px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                value={editUsername}
                onChange={(e) => setEditUsername(e.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs text-text-secondary">邮箱</span>
              <input
                className="px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                type="email"
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs text-text-secondary">新密码（留空不修改）</span>
              <input
                className="px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                type="password"
                value={editPassword}
                onChange={(e) => setEditPassword(e.target.value)}
                placeholder="至少 6 位"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs text-text-secondary">展示昵称</span>
              <input
                className="px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary"
                value={editDisplayName}
                onChange={(e) => setEditDisplayName(e.target.value)}
              />
            </label>
          </div>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-text-secondary">个人简介</span>
            <textarea
              className="px-3 py-2 text-sm border border-border rounded-ui outline-none focus:border-text-primary resize-y"
              rows={2}
              value={editBio}
              onChange={(e) => setEditBio(e.target.value)}
            />
          </label>
        </div>
      )}

      {/* Heatmap */}
      <section>
        <h2 className="text-base font-semibold text-text-primary m-0 mb-2">打卡热力图</h2>
        <div className="bg-white border border-border rounded-ui p-4 overflow-x-auto">
          <Heatmap data={heatmapData} />
        </div>
      </section>
    </div>
  );
}
