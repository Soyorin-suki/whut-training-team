import { getUserInitial } from "../../auth";

export default function UserAvatar({ user, size = 40 }) {
  const initial = getUserInitial(user);

  if (user?.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={user.username || ""}
        className="rounded-full object-cover flex-shrink-0"
        style={{ width: size, height: size }}
      />
    );
  }

  return (
    <span
      className="rounded-full bg-text-primary text-white flex items-center justify-center font-semibold flex-shrink-0"
      style={{ width: size, height: size, fontSize: size * 0.4 }}
    >
      {initial}
    </span>
  );
}
