import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");
    setLoading(true);
    try {
      const result = await login({
        username: username.trim(),
        password,
      });
      if (result.ok) {
        navigate("/");
      } else {
        setMessage(result.message);
      }
    } catch {
      setMessage("登录请求失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg-secondary">
      <div className="bg-white border border-border rounded-ui p-8 w-[380px] max-w-[90vw]">
        <h1 className="text-xl font-semibold text-text-primary m-0 mb-6 text-center">
          登录
        </h1>
        {message && (
          <p className="text-sm text-error bg-[#fff0f0] rounded-ui px-3 py-2 mb-4 m-0">
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
            placeholder="密码"
            required
          />
          <button
            className="mt-1 px-4 py-2 text-sm font-medium text-white bg-text-primary hover:bg-[#1b1f23] rounded-ui border-0 cursor-pointer disabled:opacity-50"
            type="submit"
            disabled={loading}
          >
            {loading ? "登录中..." : "登录"}
          </button>
        </form>
        <p className="text-sm text-text-secondary text-center mt-4 m-0">
          没有账号？{" "}
          <Link to="/register" className="text-text-primary underline">
            去注册
          </Link>
        </p>
      </div>
    </div>
  );
}
