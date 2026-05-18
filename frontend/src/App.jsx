import { useEffect, useState } from "react";
import { clearStoredAuth, getStoredAuth, setStoredAuth } from "./auth";
import HomeView from "./views/HomeView";
import MainView from "./views/MainView";
import ProblemDetailView from "./views/ProblemDetailView";

const MAIN_NAV_KEYS = new Set([
  "overview",
  "daily",
  "practice",
  "favorites",
  "leaderboard",
  "profile",
  "admin"
]);

function normalizeNav(value) {
  return MAIN_NAV_KEYS.has(value) ? value : "overview";
}

function buildHomeHash(nav = "overview") {
  const safeNav = normalizeNav(nav);
  return safeNav === "overview" ? "#/" : `#/?nav=${encodeURIComponent(safeNav)}`;
}

function buildProblemHash(problemKey, nav = "overview") {
  const base = `#/problems/${encodeURIComponent(problemKey)}`;
  const safeNav = normalizeNav(nav);
  return safeNav === "overview" ? base : `${base}?nav=${encodeURIComponent(safeNav)}`;
}

function buildRouteHash(routeName) {
  if (routeName === "login") {
    return "#/login";
  }
  if (routeName === "register") {
    return "#/register";
  }
  return buildHomeHash();
}

function getCurrentRoute() {
  const rawHash = window.location.hash || buildHomeHash();
  const hash = rawHash.startsWith("#") ? rawHash.slice(1) : rawHash;
  const [rawPath = "/", rawQuery = ""] = hash.split("?", 2);
  const query = new URLSearchParams(rawQuery);
  const nav = normalizeNav(query.get("nav"));

  if (rawPath === "/login") {
    return { name: "login" };
  }
  if (rawPath === "/register") {
    return { name: "register" };
  }
  if (rawPath.startsWith("/problems/")) {
    const rawProblemKey = rawPath.slice("/problems/".length).trim();
    if (rawProblemKey) {
      return {
        name: "problem",
        problemKey: decodeURIComponent(rawProblemKey),
        nav
      };
    }
  }
  return {
    name: "home",
    nav
  };
}

function navigateTo(hash) {
  window.location.hash = hash;
}

export default function App() {
  const [route, setRoute] = useState(getCurrentRoute);
  const [auth, setAuth] = useState(getStoredAuth);

  useEffect(() => {
    function onHashChange() {
      setRoute(getCurrentRoute());
    }

    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  useEffect(() => {
    if (auth && (route.name === "login" || route.name === "register")) {
      navigateTo(buildHomeHash());
    }
  }, [auth, route]);

  function handleAuthSuccess(nextAuth) {
    setStoredAuth(nextAuth);
    setAuth(nextAuth);
    navigateTo(buildHomeHash());
  }

  function handleLogout() {
    clearStoredAuth();
    setAuth(null);
    navigateTo(buildHomeHash());
  }

  function handleUserUpdate(updatedUser) {
    setAuth((prev) => {
      if (!prev) {
        return prev;
      }

      const nextAuth = {
        ...prev,
        user: {
          ...prev.user,
          ...updatedUser
        }
      };
      setStoredAuth(nextAuth);
      return nextAuth;
    });
  }

  if (route.name === "problem") {
    return (
      <ProblemDetailView
        auth={auth}
        problemKey={route.problemKey}
        onBack={() => navigateTo(buildHomeHash(route.nav))}
        onNavigate={(nextRoute) => navigateTo(buildRouteHash(nextRoute))}
      />
    );
  }

  if (!auth) {
    return (
      <HomeView
        initialPage={route.name === "register" ? "register" : "login"}
        onAuthSuccess={handleAuthSuccess}
        onNavigate={(nextRoute) => navigateTo(buildRouteHash(nextRoute))}
      />
    );
  }

  return (
    <MainView
      auth={auth}
      initialNav={route.nav}
      onLogout={handleLogout}
      onUserUpdate={handleUserUpdate}
      onNavigate={(nextRoute) => navigateTo(buildRouteHash(nextRoute))}
      onOpenProblem={(problemKey, nav) => navigateTo(buildProblemHash(problemKey, nav))}
    />
  );
}
