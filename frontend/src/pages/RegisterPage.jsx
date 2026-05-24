import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");

    if (password !== confirmPassword) {
      setMessage("两次输入的密码不一致");
      return;
    }

    setLoading(true);
    try {
      const result = await register({
        username: username.trim(),
        password,
        email: email.trim() || `${username.trim()}@whut.local`,
      });
      if (result.ok) {
        setMessage("注册成功，请登录");
        setTimeout(() => navigate("/login"), 1000);
      } else {
        setMessage(result.message);
      }
    } catch {
      setMessage("注册请求失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg-secondary">
      <div className="bg-white border border-border rounded-ui p-8 w-[380px] max-w-[90vw]">
        <h1 className="text-xl font-semibold text-text-primary m-0 mb-6 text-center">
          注册
        </h1>
        {message && (
          <p
            className={`text-sm rounded-ui px-3 py-2 mb-4 m-0 ${
              message.includes("成功")
                ? "bg-[#f0fff0] text-success"
                : "bg-[#fff0f0] text-error"
            }`}
          >
            {message}
          </p>
        )}
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <input
            className="px-3 py-2 text-sm border border-border rounded-ui bg-white text-text-primary outline-none focus:border-text-primary"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Codeforces 用户名"
            required
          />
          <input
            className="px-3 py-2 text-sm border border-border rounded-ui bg-white text-text-primary outline-none focus:border-text-primary"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="密码（至少 6 位）"
            required
          />
          <input
            className="px-3 py-2 text-sm border border-border rounded-ui bg-white text-text-primary outline-none focus:border-text-primary"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="确认密码"
            required
          />
          <input
            className="px-3 py-2 text-sm border border-border rounded-ui bg-white text-text-primary outline-none focus:border-text-primary"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="邮箱（选填）"
          />
          <button
            className="mt-1 px-4 py-2 text-sm font-medium text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer disabled:opacity-50"
            type="submit"
            disabled={loading}
          >
            {loading ? "注册中..." : "注册"}
          </button>
        </form>
        <p className="text-sm text-text-secondary text-center mt-4 m-0">
          已有账号？{" "}
          <Link to="/login" className="text-text-primary underline">
            返回登录
          </Link>
        </p>
      </div>
    </div>
  );
}
