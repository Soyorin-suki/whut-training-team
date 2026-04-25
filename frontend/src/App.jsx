import { useEffect, useState } from "react";
import { clearStoredAuth, getStoredAuth, setStoredAuth } from "./auth";
import HomeView from "./views/HomeView";
import MainView from "./views/MainView";

const ROUTES = {
  home: "#/",
  login: "#/login",
  register: "#/register"
};

function getCurrentRoute() {
  const hash = window.location.hash || ROUTES.home;
  if (hash === ROUTES.login) {
    return "login";
  }
  if (hash === ROUTES.register) {
    return "register";
  }
  return "home";
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

  function handleAuthSuccess(nextAuth) {
    setStoredAuth(nextAuth);
    setAuth(nextAuth);
    navigateTo(ROUTES.home);
  }

  function handleLogout() {
    clearStoredAuth();
    setAuth(null);
    navigateTo(ROUTES.login);
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

  if (!auth && route === "home") {
    navigateTo(ROUTES.login);
    return (
      <HomeView
        initialPage="login"
        onAuthSuccess={handleAuthSuccess}
        onNavigate={(nextRoute) => navigateTo(ROUTES[nextRoute])}
      />
    );
  }

  if (route === "login" || route === "register" || !auth) {
    return (
      <HomeView
        initialPage={route === "register" ? "register" : "login"}
        onAuthSuccess={handleAuthSuccess}
        onNavigate={(nextRoute) => navigateTo(ROUTES[nextRoute])}
      />
    );
  }

  return (
    <MainView
      auth={auth}
      onLogout={handleLogout}
      onUserUpdate={handleUserUpdate}
      onNavigate={(nextRoute) => navigateTo(ROUTES[nextRoute])}
    />
  );
}
