const AUTH_STORAGE_KEY = "whut-training-auth";

function formatDisplayDateTime(value) {
  let date = null;
  if (typeof value === "number" && Number.isFinite(value)) {
    date = new Date(value * 1000);
  } else if (typeof value === "string" && value.trim()) {
    date = new Date(value);
  }
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return "";
  }

  const pad = (value) => String(value).padStart(2, "0");
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function normalizeAuth(auth) {
  if (!auth?.user) {
    return auth;
  }
  return {
    ...auth,
    user: {
      ...auth.user,
      lastOnlineTimeIso: formatDisplayDateTime(
        auth.user.lastOnlineTimeSeconds ?? auth.user.lastOnlineTimeIso
      )
    }
  };
}

export function getStoredAuth() {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return normalizeAuth(JSON.parse(raw));
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function setStoredAuth(auth) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearStoredAuth() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function buildAuthFromLogin(data) {
  if (!data?.accessToken || !data?.refreshToken) {
    return null;
  }

  const lastOnline = data.lastOnlineTimeSeconds ?? null;
  const lastOnlineIso = formatDisplayDateTime(lastOnline ?? data.lastOnlineTimeIso);

  return {
    tokens: {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken
    },
    user: {
      id: data.id ?? data.userId ?? null,
      username: data.username ?? "",
      email: data.email ?? "",
      role: data.role ?? "",
      uid: data.uid ?? null,
      codeforcesRating: data.codeforcesRating ?? null,
      maxRating: data.maxRating ?? null,
      online: data.online ?? null,
      lastOnlineTimeSeconds: lastOnline,
      lastOnlineTimeIso: lastOnlineIso,
      avatarUrl: data.avatarUrl ?? ""
    }
  };
}

export function getUserInitial(user) {
  const source = user?.username?.trim() || user?.email?.trim() || "?";
  return source.charAt(0).toUpperCase();
}
