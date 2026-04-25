import { useEffect, useState } from "react";
import { buildAuthFromLogin } from "../auth";
import { login, registerUser } from "../api/user";

const LOGIN = "login";
const REGISTER = "register";

export default function HomeView({ initialPage = LOGIN, onAuthSuccess, onNavigate }) {
  const [page, setPage] = useState(initialPage);
  const [message, setMessage] = useState("");
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [registerForm, setRegisterForm] = useState({
    username: "",
    password: "",
    confirmPassword: "",
    email: ""
  });

  useEffect(() => {
    setPage(initialPage);
    setMessage("");
  }, [initialPage]);

  async function onLogin(event) {
    event.preventDefault();
    setMessage("");

    try {
      const resp = await login({
        username: loginForm.username.trim(),
        password: loginForm.password
      });
      if (resp.code !== 200) {
        setMessage(resp.message || "登录失败");
        return;
      }

      const auth = buildAuthFromLogin(resp.data);
      if (!auth) {
        setMessage("登录成功，但凭证不完整");
        return;
      }
      onAuthSuccess?.(auth);
    } catch (error) {
      setMessage(error.response?.data?.message || "登录请求失败");
    }
  }

  async function onRegister(event) {
    event.preventDefault();
    setMessage("");

    const username = registerForm.username.trim();
    const password = registerForm.password;
    const confirmPassword = registerForm.confirmPassword;
    const email = registerForm.email.trim() || `${username}@whut.local`;

    if (password !== confirmPassword) {
      setMessage("两次输入的密码不一致");
      return;
    }

    try {
      const resp = await registerUser({ username, password, email });
      if (resp.code !== 200) {
        setMessage(resp.message || "注册失败");
        return;
      }
      setMessage("注册成功，请登录");
      setPage(LOGIN);
      onNavigate?.(LOGIN);
      setLoginForm({ username, password: "" });
      setRegisterForm({ username: "", password: "", confirmPassword: "", email: "" });
    } catch (error) {
      setMessage(error.response?.data?.message || "注册请求失败");
    }
  }

  function switchPage(nextPage) {
    setMessage("");
    setPage(nextPage);
    onNavigate?.(nextPage);
  }

  return (
    <main className="page">
      <section className="auth-card">
        <h1 className="auth-title">{page === LOGIN ? "登录" : "注册"}</h1>
        {message && <p className="auth-message">{message}</p>}

        {page === LOGIN ? (
          <form className="auth-form" onSubmit={onLogin}>
            <input
              className="auth-input"
              value={loginForm.username}
              onChange={(event) =>
                setLoginForm((prev) => ({ ...prev, username: event.target.value }))
              }
              placeholder="Codeforces 用户名"
              required
            />
            <input
              className="auth-input"
              value={loginForm.password}
              onChange={(event) =>
                setLoginForm((prev) => ({ ...prev, password: event.target.value }))
              }
              type="password"
              placeholder="密码"
              required
            />
            <button className="auth-button" type="submit">
              登录
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={onRegister}>
            <input
              className="auth-input"
              value={registerForm.username}
              onChange={(event) =>
                setRegisterForm((prev) => ({ ...prev, username: event.target.value }))
              }
              placeholder="Codeforces 用户名"
              required
            />
            <input
              className="auth-input"
              value={registerForm.password}
              onChange={(event) =>
                setRegisterForm((prev) => ({ ...prev, password: event.target.value }))
              }
              type="password"
              placeholder="密码（至少 6 位）"
              required
            />
            <input
              className="auth-input"
              value={registerForm.confirmPassword}
              onChange={(event) =>
                setRegisterForm((prev) => ({ ...prev, confirmPassword: event.target.value }))
              }
              type="password"
              placeholder="确认密码"
              required
            />
            <input
              className="auth-input"
              value={registerForm.email}
              onChange={(event) =>
                setRegisterForm((prev) => ({ ...prev, email: event.target.value }))
              }
              type="email"
              placeholder="邮箱（选填）"
            />
            <button className="auth-button" type="submit">
              注册
            </button>
          </form>
        )}

        {page === LOGIN ? (
          <button className="switch-link" type="button" onClick={() => switchPage(REGISTER)}>
            去注册
          </button>
        ) : (
          <button className="switch-link" type="button" onClick={() => switchPage(LOGIN)}>
            返回登录
          </button>
        )}
      </section>
    </main>
  );
}
