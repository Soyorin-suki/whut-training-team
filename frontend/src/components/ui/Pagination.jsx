export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-2 mt-4">
      <button
        className="px-3 py-1.5 text-nav border border-border rounded-ui bg-white text-text-primary hover:bg-bg-secondary disabled:opacity-40 disabled:cursor-not-allowed"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        上一页
      </button>
      <span className="text-nav text-text-secondary px-2">
        {page} / {totalPages}
      </span>
      <button
        className="px-3 py-1.5 text-nav border border-border rounded-ui bg-white text-text-primary hover:bg-bg-secondary disabled:opacity-40 disabled:cursor-not-allowed"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        下一页
      </button>
    </div>
  );
}
