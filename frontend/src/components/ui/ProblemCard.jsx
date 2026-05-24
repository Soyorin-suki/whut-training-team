import { getRatingMeta, parseTags } from "../../utils/cf";

export default function ProblemCard({ problem }) {
  if (!problem) return null;
  const meta = getRatingMeta(problem.rating);
  const tags = parseTags(problem.tags);

  return (
    <article className="bg-white border border-border rounded-ui p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <h3 className="text-base font-semibold text-text-primary m-0 truncate">
            {problem.contestId}
            {problem.problemIndex}. {problem.name}
          </h3>
          {problem.date && (
            <p className="text-xs text-text-secondary mt-1 m-0">
              {problem.date}{problem.type ? ` · ${problem.type}` : ""}
            </p>
          )}
        </div>
        <span
          className="text-sm font-bold flex-shrink-0"
          style={{ color: meta.color }}
        >
          {problem.rating ?? "未定级"}
        </span>
      </div>
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mt-2.5">
          {tags.map((t) => (
            <span
              key={t}
              className="text-xs px-2 py-0.5 rounded-full bg-bg-secondary text-text-secondary"
            >
              {t}
            </span>
          ))}
        </div>
      )}
      {problem.sourceUrl && (
        <a
          className="inline-block mt-2.5 text-xs text-text-secondary hover:text-text-primary underline"
          href={problem.sourceUrl}
          target="_blank"
          rel="noreferrer"
        >
          在 Codeforces 打开 →
        </a>
      )}
    </article>
  );
}
