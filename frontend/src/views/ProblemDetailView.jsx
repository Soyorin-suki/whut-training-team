import { useEffect, useState } from "react";
import { getUserInitial } from "../auth";
import {
  createProblemComment,
  favoriteProblem,
  getProblemComments,
  getProblemDetail,
  likeProblem,
  unfavoriteProblem,
  unlikeProblem
} from "../api/dailyProblem";

function parseTags(tags) {
  if (!tags) {
    return [];
  }

  return String(tags)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 8);
}

function normalizeLikeCount(likeCount) {
  return Number.isFinite(Number(likeCount)) ? Math.max(0, Number(likeCount)) : 0;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  });
}

function getDisplayInitial(text) {
  const value = String(text || "").trim();
  return value ? value.charAt(0).toUpperCase() : "?";
}

function countProblemComments(items) {
  return (items || []).reduce(
    (sum, item) => sum + 1 + ((item?.replies || []).length || 0),
    0
  );
}

function ProblemLikeButton({ problem, pending = false, onToggleLike }) {
  if (!problem?.problemKey) {
    return null;
  }

  const likedByMe = Boolean(problem.likedByMe);
  const likeCount = normalizeLikeCount(problem.likeCount);

  return (
    <button
      className={`problem-like-button ${likedByMe ? "is-liked" : ""}`}
      type="button"
      disabled={pending}
      onClick={() => onToggleLike?.(problem)}
    >
      <span className="problem-like-icon" aria-hidden="true">
        {likedByMe ? "♥" : "♡"}
      </span>
      <span>{likeCount}</span>
      {pending ? <span className="problem-like-pending">...</span> : null}
    </button>
  );
}

function ProblemFavoriteButton({ problem, pending = false, onToggleFavorite }) {
  if (!problem?.problemKey) {
    return null;
  }

  const favoritedByMe = Boolean(problem.favoritedByMe);

  return (
    <button
      className={`problem-favorite-button ${favoritedByMe ? "is-favorited" : ""}`}
      type="button"
      disabled={pending}
      onClick={() => onToggleFavorite?.(problem)}
    >
      <span className="problem-favorite-icon" aria-hidden="true">
        {favoritedByMe ? "★" : "☆"}
      </span>
      <span>{favoritedByMe ? "已收藏" : "未收藏"}</span>
      {pending ? <span className="problem-favorite-pending">...</span> : null}
    </button>
  );
}

function CommentAvatar({ author }) {
  if (author?.avatarUrl) {
    return <img className="comment-avatar" src={author.avatarUrl} alt={`${author.username || "User"} avatar`} />;
  }

  return (
    <span className="comment-avatar comment-avatar-fallback">
      {getDisplayInitial(author?.username)}
    </span>
  );
}

function ReplyComposer({
  value,
  onChange,
  onSubmit,
  onCancel,
  submitting = false,
  placeholder = "Write a reply"
}) {
  return (
    <div className="comment-reply-composer">
      <textarea
        className="auth-input comment-input"
        rows={3}
        maxLength={1000}
        value={value}
        onChange={(event) => onChange?.(event.target.value)}
        placeholder={placeholder}
      />
      <div className="comment-composer-actions">
        <button className="ghost-button" type="button" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button className="primary-button" type="button" onClick={onSubmit} disabled={submitting}>
          {submitting ? "Posting..." : "Post reply"}
        </button>
      </div>
    </div>
  );
}

function CommentList({
  comments,
  replyTarget,
  replyDraft,
  onReplyDraftChange,
  onReplyOpen,
  onReplyCancel,
  onReplySubmit,
  submitting,
  canReply
}) {
  return (
    <div className="comment-thread-list">
      {(comments || []).map((comment) => (
        <article className="comment-thread" key={comment.id}>
          <div className="comment-card">
            <CommentAvatar author={comment.author} />
            <div className="comment-main">
              <div className="comment-meta">
                <div>
                  <strong>{comment.author?.username || "Unknown user"}</strong>
                  <span>{formatDateTime(comment.createdAt)}</span>
                </div>
                {canReply ? (
                  <button className="ghost-button comment-reply-button" type="button" onClick={() => onReplyOpen?.(comment)}>
                    {replyTarget?.id === comment.id ? "Cancel" : "Reply"}
                  </button>
                ) : null}
              </div>
              <p className="comment-content">{comment.content}</p>
            </div>
          </div>

          {replyTarget?.id === comment.id ? (
            <ReplyComposer
              value={replyDraft}
              onChange={onReplyDraftChange}
              onSubmit={onReplySubmit}
              onCancel={onReplyCancel}
              submitting={submitting}
              placeholder={`Reply @${replyTarget.username || "user"}`}
            />
          ) : null}

          {(comment.replies || []).length > 0 ? (
            <div className="comment-reply-list">
              {comment.replies.map((reply) => (
                <div className="comment-reply-item" key={reply.id}>
                  <div className="comment-card is-reply">
                    <CommentAvatar author={reply.author} />
                    <div className="comment-main">
                      <div className="comment-meta">
                        <div>
                          <strong>{reply.author?.username || "Unknown user"}</strong>
                          <span>{formatDateTime(reply.createdAt)}</span>
                        </div>
                        {canReply ? (
                          <button className="ghost-button comment-reply-button" type="button" onClick={() => onReplyOpen?.(reply)}>
                            {replyTarget?.id === reply.id ? "Cancel" : "Reply"}
                          </button>
                        ) : null}
                      </div>
                      {reply.replyToUsername ? (
                        <span className="comment-reply-to">Reply to @{reply.replyToUsername}</span>
                      ) : null}
                      <p className="comment-content">{reply.content}</p>
                    </div>
                  </div>

                  {replyTarget?.id === reply.id ? (
                    <ReplyComposer
                      value={replyDraft}
                      onChange={onReplyDraftChange}
                      onSubmit={onReplySubmit}
                      onCancel={onReplyCancel}
                      submitting={submitting}
                      placeholder={`Reply @${replyTarget.username || "user"}`}
                    />
                  ) : null}
                </div>
              ))}
            </div>
          ) : null}
        </article>
      ))}
    </div>
  );
}

export default function ProblemDetailView({
  auth,
  problemKey,
  onBack,
  onNavigate
}) {
  const [problem, setProblem] = useState(null);
  const [comments, setComments] = useState([]);
  const [message, setMessage] = useState("");
  const [detailLoading, setDetailLoading] = useState(false);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [commentDraft, setCommentDraft] = useState("");
  const [replyDraft, setReplyDraft] = useState("");
  const [replyTarget, setReplyTarget] = useState(null);
  const [likePending, setLikePending] = useState(false);
  const [favoritePending, setFavoritePending] = useState(false);

  const user = auth?.user ?? null;
  const tokens = auth?.tokens ?? null;
  const totalComments = countProblemComments(comments);

  useEffect(() => {
    setReplyTarget(null);
    setReplyDraft("");
    setCommentDraft("");
  }, [problemKey]);

  useEffect(() => {
    if (!problemKey) {
      return;
    }
    void loadProblemData();
  }, [problemKey, user?.id, tokens?.accessToken, tokens?.refreshToken]);

  async function loadProblemData() {
    setDetailLoading(true);
    setCommentsLoading(true);
    try {
      const [detailResp, commentsResp] = await Promise.allSettled([
        getProblemDetail(problemKey, tokens),
        getProblemComments(problemKey, tokens)
      ]);
      let nextMessage = "";

      if (detailResp.status === "fulfilled") {
        if (detailResp.value.code === 200) {
          setProblem(detailResp.value.data || null);
        } else {
          setProblem(null);
          nextMessage = detailResp.value.message || "Failed to load problem detail";
        }
      } else {
        setProblem(null);
        nextMessage = detailResp.reason?.response?.data?.message || "Failed to load problem detail";
      }

      if (commentsResp.status === "fulfilled") {
        if (commentsResp.value.code === 200) {
          setComments(commentsResp.value.data || []);
        } else if (!nextMessage) {
          nextMessage = commentsResp.value.message || "Failed to load comments";
        }
      } else if (!nextMessage) {
        nextMessage = commentsResp.reason?.response?.data?.message || "Failed to load comments";
      }

      setMessage(nextMessage);
    } finally {
      setDetailLoading(false);
      setCommentsLoading(false);
    }
  }

  async function refreshComments() {
    if (!problemKey) {
      return;
    }
    setCommentsLoading(true);
    try {
      const resp = await getProblemComments(problemKey, tokens);
      if (resp.code === 200) {
        setComments(resp.data || []);
      } else {
        setMessage(resp.message || "Failed to load comments");
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "Failed to load comments");
    } finally {
      setCommentsLoading(false);
    }
  }

  function requireLogin(actionText) {
    setMessage(actionText);
  }

  async function handleToggleLike(currentProblem) {
    if (!tokens || !currentProblem?.problemKey || likePending) {
      if (!tokens) {
        requireLogin("Please login first to like this problem");
      }
      return;
    }

    const previous = {
      likeCount: normalizeLikeCount(currentProblem.likeCount),
      likedByMe: Boolean(currentProblem.likedByMe)
    };
    const optimisticLiked = !previous.likedByMe;
    const optimistic = {
      ...currentProblem,
      likeCount: Math.max(0, previous.likeCount + (optimisticLiked ? 1 : -1)),
      likedByMe: optimisticLiked
    };

    setLikePending(true);
    setMessage("");
    setProblem(optimistic);
    try {
      const resp = optimisticLiked
        ? await likeProblem(currentProblem.problemKey, tokens)
        : await unlikeProblem(currentProblem.problemKey, tokens);
      if (resp.code !== 200) {
        setProblem((value) => value ? { ...value, ...previous } : value);
        setMessage(resp.message || "Failed to update like");
        return;
      }
      setProblem((value) => value ? { ...value, ...resp.data } : value);
    } catch (error) {
      setProblem((value) => value ? { ...value, ...previous } : value);
      setMessage(error.response?.data?.message || "Failed to update like");
    } finally {
      setLikePending(false);
    }
  }

  async function handleToggleFavorite(currentProblem) {
    if (!tokens || !currentProblem?.problemKey || favoritePending) {
      if (!tokens) {
        requireLogin("Please login first to favorite this problem");
      }
      return;
    }

    const previous = {
      favoritedByMe: Boolean(currentProblem.favoritedByMe),
      favoritedAt: currentProblem.favoritedAt || null
    };
    const optimisticFavorited = !previous.favoritedByMe;
    const optimistic = {
      ...currentProblem,
      favoritedByMe: optimisticFavorited,
      favoritedAt: optimisticFavorited
        ? currentProblem.favoritedAt || new Date().toISOString()
        : null
    };

    setFavoritePending(true);
    setMessage("");
    setProblem(optimistic);
    try {
      const resp = optimisticFavorited
        ? await favoriteProblem(currentProblem.problemKey, tokens)
        : await unfavoriteProblem(currentProblem.problemKey, tokens);
      if (resp.code !== 200) {
        setProblem((value) => value ? { ...value, ...previous } : value);
        setMessage(resp.message || "Failed to update favorite");
        return;
      }
      setProblem((value) => value ? { ...value, ...resp.data } : value);
    } catch (error) {
      setProblem((value) => value ? { ...value, ...previous } : value);
      setMessage(error.response?.data?.message || "Failed to update favorite");
    } finally {
      setFavoritePending(false);
    }
  }

  function handleOpenReply(comment) {
    if (!user) {
      requireLogin("Please login first to reply");
      return;
    }
    if (!comment?.id) {
      return;
    }
    setMessage("");
    setReplyTarget((current) => (
      current?.id === comment.id
        ? null
        : {
          id: comment.id,
          username: comment.author?.username || "user"
        }
    ));
    if (replyTarget?.id === comment.id) {
      setReplyDraft("");
    }
  }

  function handleCancelReply() {
    setReplyTarget(null);
    setReplyDraft("");
  }

  async function handleCreateRootComment() {
    if (!tokens) {
      requireLogin("Please login first to post a comment");
      return;
    }
    if (!problem?.problemKey) {
      return;
    }
    if (!commentDraft.trim()) {
      setMessage("Comment content is required");
      return;
    }

    setCommentSubmitting(true);
    setMessage("");
    try {
      const resp = await createProblemComment(
        {
          problemKey: problem.problemKey,
          content: commentDraft,
          replyCommentId: null
        },
        tokens
      );
      if (resp.code !== 200) {
        setMessage(resp.message || "Failed to post comment");
        return;
      }
      setCommentDraft("");
      await refreshComments();
    } catch (error) {
      setMessage(error.response?.data?.message || "Failed to post comment");
    } finally {
      setCommentSubmitting(false);
    }
  }

  async function handleCreateReplyComment() {
    if (!tokens) {
      requireLogin("Please login first to reply");
      return;
    }
    if (!problem?.problemKey || !replyTarget?.id) {
      return;
    }
    if (!replyDraft.trim()) {
      setMessage("Reply content is required");
      return;
    }

    setCommentSubmitting(true);
    setMessage("");
    try {
      const resp = await createProblemComment(
        {
          problemKey: problem.problemKey,
          content: replyDraft,
          replyCommentId: replyTarget.id
        },
        tokens
      );
      if (resp.code !== 200) {
        setMessage(resp.message || "Failed to post reply");
        return;
      }
      setReplyDraft("");
      setReplyTarget(null);
      await refreshComments();
    } catch (error) {
      setMessage(error.response?.data?.message || "Failed to post reply");
    } finally {
      setCommentSubmitting(false);
    }
  }

  return (
    <main className="portal-shell">
      <header className="site-header portal-header">
        <div className="site-brand">
          <span className="site-brand-mark">WHUT</span>
          <div>
            <strong>WHUT Training Portal</strong>
            <span>Problem detail and shared discussion thread</span>
          </div>
        </div>

        <div className="user-toolbar">
          <button className="ghost-button" type="button" onClick={onBack}>
            返回题目列表
          </button>
          {user ? (
            <div className="user-chip">
              {user.avatarUrl ? (
                <img className="avatar-image" src={user.avatarUrl} alt="avatar" />
              ) : (
                <span className="avatar-badge">{getUserInitial(user)}</span>
              )}
              <div>
                <strong>{user.username || "-"}</strong>
                <span>{user.role || "USER"}</span>
              </div>
            </div>
          ) : (
            <div className="site-actions">
              <button className="ghost-button" type="button" onClick={() => onNavigate?.("login")}>
                登录
              </button>
              <button className="primary-button" type="button" onClick={() => onNavigate?.("register")}>
                注册
              </button>
            </div>
          )}
        </div>
      </header>

      <section className="portal-page">
        <article className="portal-hero problem-detail-hero">
          <div className="portal-hero-copy">
            <p className="section-eyebrow">Problem Detail</p>
            <h1>{problem?.name || problemKey || "Problem"}</h1>
            <p>
              同一道题在每日题、历史记录、自主练习和收藏页中共享同一条评论线程。
              进入题目详情后可以集中查看题面信息、点赞收藏状态和讨论内容。
            </p>
          </div>
          <div className="portal-hero-aside">
            <div className="status-pill is-warning">{problem?.rating ?? "Unrated"}</div>
            <strong>{problem?.problemKey || problemKey || "-"}</strong>
            <span>{totalComments} comments</span>
          </div>
        </article>

        {message ? <p className="system-message">{message}</p> : null}

        <div className="portal-grid problem-detail-grid">
          <div className="portal-main-column">
            <section className="section-card">
              <div className="section-heading section-heading-inline">
                <div>
                  <p className="section-eyebrow">Problem</p>
                  <h2>题目信息</h2>
                </div>
                {detailLoading ? <span className="favorite-page-meta">Loading...</span> : null}
              </div>

              {!problem ? (
                <p className="empty-copy">
                  {detailLoading ? "正在加载题目详情..." : "未能加载该题目的详情信息。"}
                </p>
              ) : (
                <div className="problem-detail-layout">
                  <div className="problem-detail-header">
                    <div>
                      <h3>
                        {problem.contestId}
                        {problem.problemIndex}. {problem.name}
                      </h3>
                      <p className="problem-detail-summary">
                        {problem.problemKey} · rating {problem.rating ?? "Unrated"}
                        {problem.favoritedByMe && problem.favoritedAt
                          ? ` · collected at ${formatDateTime(problem.favoritedAt)}`
                          : ""}
                      </p>
                    </div>
                    <div className="problem-detail-actions">
                      <span className="problem-rating-badge">{problem.rating ?? "Unrated"}</span>
                      <div className="problem-action-stack">
                        <ProblemLikeButton problem={problem} pending={likePending} onToggleLike={handleToggleLike} />
                        <ProblemFavoriteButton
                          problem={problem}
                          pending={favoritePending}
                          onToggleFavorite={handleToggleFavorite}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="problem-detail-meta">
                    <span>Contest: {problem.contestId ?? "-"}</span>
                    <span>Index: {problem.problemIndex || "-"}</span>
                    <span>Comments: {totalComments}</span>
                  </div>

                  <div className="tag-list">
                    {parseTags(problem.tags).map((tag) => (
                      <span className="tag" key={tag}>
                        {tag}
                      </span>
                    ))}
                  </div>

                  <div className="problem-detail-link-row">
                    <a className="text-link" href={problem.sourceUrl} target="_blank" rel="noreferrer">
                      前往 Codeforces 查看题面
                    </a>
                  </div>
                </div>
              )}
            </section>

            <section className="section-card">
              <div className="section-heading section-heading-inline">
                <div>
                  <p className="section-eyebrow">Comments</p>
                  <h2>通用评论区</h2>
                  <p>当前评论按题目归属，不再区分每日题实例。</p>
                </div>
                <div className="favorite-page-meta">
                  <span>{totalComments} comments</span>
                  <span>{commentsLoading ? "Syncing" : "Ready"}</span>
                </div>
              </div>

              {user ? (
                <div className="comment-compose-card">
                  <textarea
                    className="auth-input comment-input"
                    rows={4}
                    maxLength={1000}
                    value={commentDraft}
                    onChange={(event) => setCommentDraft(event.target.value)}
                    placeholder="Share your thoughts about this problem"
                  />
                  <div className="comment-composer-actions">
                    <span className="comment-char-count">{String(commentDraft || "").length}/1000</span>
                    <button className="primary-button" type="button" onClick={handleCreateRootComment} disabled={commentSubmitting}>
                      {commentSubmitting ? "Posting..." : "Post comment"}
                    </button>
                  </div>
                </div>
              ) : (
                <p className="comment-readonly-note">
                  你当前未登录。可以先查看题目与已有讨论，登录后再发表评论、回复、点赞或收藏。
                </p>
              )}

              {commentsLoading ? (
                <p className="empty-copy">正在加载评论...</p>
              ) : comments.length === 0 ? (
                <p className="empty-copy">当前还没有评论，欢迎成为第一个发言的人。</p>
              ) : (
                <CommentList
                  comments={comments}
                  replyTarget={replyTarget}
                  replyDraft={replyDraft}
                  onReplyDraftChange={setReplyDraft}
                  onReplyOpen={handleOpenReply}
                  onReplyCancel={handleCancelReply}
                  onReplySubmit={handleCreateReplyComment}
                  submitting={commentSubmitting}
                  canReply={Boolean(user)}
                />
              )}
            </section>
          </div>

          <div className="portal-side-column">
            <section className="section-card side-info-card">
              <div className="section-heading">
                <p className="section-eyebrow">Guide</p>
                <h3>使用说明</h3>
              </div>
              <div className="notice-list">
                <article className="notice-item">
                  <strong>统一入口</strong>
                  <p>从每日题、历史记录、自主练习和收藏页进入同一道题，都会落到当前详情路由。</p>
                </article>
                <article className="notice-item">
                  <strong>共享线程</strong>
                  <p>旧的每日题实例评论已经按 `problemKey` 合并，后续讨论都以题目为单位持续累积。</p>
                </article>
                <article className="notice-item">
                  <strong>互动权限</strong>
                  <p>未登录用户可以查看题目和评论内容；发表评论、回复、点赞和收藏需要先登录。</p>
                </article>
              </div>
            </section>
          </div>
        </div>
      </section>
    </main>
  );
}
