const AUTH_STORAGE_KEY = "whut-training-auth";

export function getStoredAuth() {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
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

  return {
    tokens: {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken
    },
    user: {
      id: data.userId ?? null,
      username: data.username ?? "",
      email: data.email ?? "",
      role: data.role ?? ""
    }
  };
}

export function getUserInitial(user) {
  const source = user?.username?.trim() || user?.email?.trim() || "?";
  return source.charAt(0).toUpperCase();
}
