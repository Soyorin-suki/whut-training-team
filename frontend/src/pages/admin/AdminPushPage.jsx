import { useEffect, useState } from "react";
import { listPush, approvePushItem, rejectPushItem, promotePushItem, deletePushItem, getPushPool } from "../../api/push";
import EmptyState from "../../components/ui/EmptyState";
import { ListSkeleton } from "../../components/ui/Skeleton";

export default function AdminPushPage() {
  const [items, setItems] = useState([]);
  const [poolItems, setPoolItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [deletingId, setDeletingId] = useState(null);

  async function load() {
    try {
      const [listResp, poolResp] = await Promise.all([
        listPush(),
        getPushPool(),
      ]);
      if (listResp.code === 200) setItems(listResp.data || []);
      if (poolResp.code === 200) setPoolItems(poolResp.data || []);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleApprove(id) {
    try {
      const resp = await approvePushItem(id);
      if (resp.code === 200) load();
      else setMessage(resp.message || "操作失败");
    } catch { setMessage("请求失败"); }
  }

  async function handleReject(id) {
    try {
      const resp = await rejectPushItem(id);
      if (resp.code === 200) load();
      else setMessage(resp.message || "操作失败");
    } catch { setMessage("请求失败"); }
  }

  async function handlePromote(id) {
    try {
      const resp = await promotePushItem(id);
      if (resp.code === 200) load();
      else setMessage(resp.message || "操作失败");
    } catch { setMessage("请求失败"); }
  }

  async function handleDelete(id) {
    if (!window.confirm("确定要删除该推题吗？")) return;
    setDeletingId(id);
    try {
      const resp = await deletePushItem(id);
      if (resp.code === 200) {
        load();
        setMessage("删除成功");
      } else {
        setMessage(resp.message || "删除失败");
      }
    } catch {
      setMessage("请求失败");
    } finally {
      setDeletingId(null);
    }
  }

  const pending = items.filter((i) => i.status === "PENDING");
  const approved = items.filter((i) => i.status === "APPROVED");
  const rejected = items.filter((i) => i.status === "REJECTED");

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-text-primary m-0">推题审核</h1>

      {message && (
        <p className="text-sm bg-bg-secondary text-text-secondary rounded-ui px-3 py-2 m-0">{message}</p>
      )}

      {loading ? (
        <ListSkeleton rows={5} />
      ) : (
        <>
          {/* Pending */}
          <section>
            <h2 className="text-base font-semibold text-text-primary m-0 mb-2">
              待审核 ({pending.length})
            </h2>
            {pending.length === 0 ? (
              <EmptyState title="暂无待审核推题" />
            ) : (
              <div className="space-y-2">
                {pending.map((item) => (
                  <div key={item.id} className="bg-white border border-border rounded-ui p-3 flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-text-primary m-0">{item.title}</p>
                      {item.link && (
                        <a href={item.link} target="_blank" rel="noreferrer" className="text-xs text-text-secondary underline">
                          {item.link}
                        </a>
                      )}
                      {item.description && <p className="text-xs text-text-secondary mt-0.5 m-0">{item.description}</p>}
                    </div>
                    <div className="flex gap-1 flex-shrink-0">
                      <button className="px-2.5 py-1 text-xs font-medium text-white bg-success hover:bg-[#268845] rounded border-0 cursor-pointer"
                        onClick={() => handleApprove(item.id)}>通过</button>
                      <button className="px-2.5 py-1 text-xs font-medium text-white bg-error hover:bg-[#b01e28] rounded border-0 cursor-pointer"
                        onClick={() => handleReject(item.id)}>拒绝</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Pool - approved but unpublished */}
          <section>
            <h2 className="text-base font-semibold text-text-primary m-0 mb-2">
              推题池 · 待推送 ({poolItems.length})
            </h2>
            {poolItems.length === 0 ? (
              <EmptyState title="推题池为空" description="审核通过推题后，未被推送的题目会出现在这里" />
            ) : (
              <div className="space-y-2">
                {poolItems.map((item) => (
                  <div key={item.id} className="bg-white border border-border rounded-ui p-3 flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-text-primary m-0">{item.title}</p>
                    </div>
                    <div className="flex gap-1 flex-shrink-0">
                      <button className="px-2.5 py-1 text-xs border border-border rounded bg-white text-text-secondary hover:bg-bg-secondary cursor-pointer"
                        onClick={() => handlePromote(item.id)}>提升</button>
                      <button className="px-2.5 py-1 text-xs font-medium text-white bg-error hover:bg-[#b01e28] rounded border-0 cursor-pointer disabled:opacity-40"
                        onClick={() => handleDelete(item.id)}
                        disabled={deletingId === item.id}>
                        {deletingId === item.id ? "..." : "删除"}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Approved */}
          <section>
            <h2 className="text-base font-semibold text-text-primary m-0 mb-2">
              已通过 ({approved.length})
            </h2>
            {approved.length === 0 ? (
              <EmptyState title="暂无已通过推题" />
            ) : (
              <div className="space-y-2">
                {approved.map((item) => (
                  <div key={item.id} className="bg-white border border-border rounded-ui p-3 flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-text-primary m-0">{item.title}</p>
                    </div>
                    <button className="px-2.5 py-1 text-xs border border-border rounded bg-white text-text-secondary hover:bg-bg-secondary cursor-pointer"
                      onClick={() => handlePromote(item.id)}>提升</button>
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
