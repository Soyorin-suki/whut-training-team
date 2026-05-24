export default function EmptyState({ icon, title, description }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      {icon && <div className="text-text-secondary mb-3">{icon}</div>}
      <p className="text-text-primary font-medium m-0">{title || "暂无数据"}</p>
      {description && (
        <p className="text-sm text-text-secondary mt-1 m-0">{description}</p>
      )}
    </div>
  );
}
