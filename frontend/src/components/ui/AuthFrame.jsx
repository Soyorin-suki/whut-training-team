import { CircleDot, MoveUpRight } from "lucide-react";
import { Link } from "react-router-dom";

export default function AuthFrame({ title, subtitle, children, footer }) {
  return (
    <main className="auth-shell">
      <div className="auth-geometry" aria-hidden="true">
        <span className="geometry-ring geometry-ring-one" />
        <span className="geometry-ring geometry-ring-two" />
        <span className="geometry-square" />
        <span className="geometry-line" />
      </div>

      <section className="auth-intro">
        <Link to="/" className="brand-lockup">
          <span className="brand-mark">
            <img src="/whut-acm-logo.png" alt="" aria-hidden="true" />
          </span>
          <span>WHUT-ACM</span>
        </Link>
        <div className="auth-intro-copy">
          <p className="eyebrow"><CircleDot size={13} /> PROGRAMMING LAB</p>
          <h2>保持训练，<br />让进步可见。</h2>
          <p>每日题目、训练记录与团队排名，集中在一个安静而专注的空间。</p>
        </div>
        <p className="auth-footnote">WHUT ACM · EST. 2026</p>
      </section>

      <section className="auth-card-wrap">
        <div className="auth-card">
          <div className="auth-card-index">/ ACCOUNT</div>
          <div className="auth-card-heading">
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
          {children}
          <div className="auth-card-footer">
            <span>{footer}</span>
            <MoveUpRight size={15} />
          </div>
        </div>
      </section>
    </main>
  );
}
