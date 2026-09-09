import { Link, Outlet, createRootRoute } from "@tanstack/react-router";

import { defineNav } from "../lib/nav";

const nav = defineNav([
  { to: "/", label: "Overview" },
  { to: "/recipes", label: "Recipes" },
]);

export const Route = createRootRoute({
  component: RootLayout,
  notFoundComponent: NotFound,
});

function RootLayout() {
  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="brand">
          far<span className="brand-dim">-kitchen</span>
        </Link>
        <nav className="nav">
          {nav.map((item) => (
            <Link key={item.to} to={item.to} activeProps={{ className: "active" }}>
              {item.label}
            </Link>
          ))}
        </nav>
        <span className="badge" title="Version reported by the installed compiler">
          tsc {__TS_VERSION__}
        </span>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <footer className="footer">
        React 19 · Vite · TanStack Router · type-checked by the native TypeScript compiler
      </footer>
    </div>
  );
}

function NotFound() {
  return (
    <section className="card">
      <h1>Nothing here</h1>
      <p className="muted">That route does not exist.</p>
      <Link to="/" className="button">
        Back to overview
      </Link>
    </section>
  );
}
