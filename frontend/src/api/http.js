import axios from "axios";
import { clearStoredAuth, getStoredAuth, setStoredAuth } from "../auth";

const http = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 5000
});

let refreshingPromise = null;

function shouldSkipRefresh(config) {
  const url = config?.url || "";
  return url.includes("/api/auth/login")
    || url.includes("/api/auth/register")
    || url.includes("/api/auth/refresh");
}

async function refreshAccessToken() {
  const auth = getStoredAuth();
  const refreshToken = auth?.tokens?.refreshToken;
  if (!refreshToken) {
    throw new Error("missing refresh token");
  }

  const response = await axios.post(
    "http://localhost:8080/api/auth/refresh",
    {},
    { headers: { "X-Refresh-Token": refreshToken } }
  );

  const payload = response?.data;
  if (payload?.code !== 200 || !payload?.data?.accessToken || !payload?.data?.refreshToken) {
    throw new Error("refresh failed");
  }

  const nextAuth = {
    ...auth,
    tokens: {
      accessToken: payload.data.accessToken,
      refreshToken: payload.data.refreshToken
    }
  };
  setStoredAuth(nextAuth);
  return nextAuth.tokens;
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error?.config;
    const status = error?.response?.status;

    if (!config || status !== 401 || config._retry || shouldSkipRefresh(config)) {
      return Promise.reject(error);
    }

    config._retry = true;

    try {
      if (!refreshingPromise) {
        refreshingPromise = refreshAccessToken().finally(() => {
          refreshingPromise = null;
        });
      }
      const tokens = await refreshingPromise;
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${tokens.accessToken}`;
      config.headers["X-Refresh-Token"] = tokens.refreshToken;
      return http(config);
    } catch (refreshError) {
      clearStoredAuth();
      return Promise.reject(refreshError);
    }
  }
);

export default http;
