import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-bg-secondary">
      <h1 className="text-4xl font-bold text-text-primary m-0">404</h1>
      <p className="text-text-secondary mt-2">页面不存在</p>
      <Link
        to="/"
        className="mt-4 px-4 py-2 text-sm text-white bg-text-primary rounded-ui hover:bg-[#1b1f23] no-underline"
      >
        返回首页
      </Link>
    </div>
  );
}
