import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "./api";

const formatINR = (n) => {
  n = Number(n || 0);
  if (n >= 1e7) return `₹${(n / 1e7).toFixed(1)}Cr`;
  if (n >= 1e5) return `₹${(n / 1e5).toFixed(1)}L`;
  if (n >= 1e3) return `₹${(n / 1e3).toFixed(0)}K`;
  return `₹${n}`;
};

const RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const STATUSES = ["APPROVED", "REVIEW", "BLOCKED"];
const EMPTY = {
  customerId: "", merchant: "", beneficiary: "", country: "",
  minAmount: "", maxAmount: "", from: "", to: "",
  riskLevel: [], status: [],
};
const PAGE_SIZE = 15;

function buildQuery(f, sort, page) {
  const p = new URLSearchParams();
  ["customerId", "merchant", "beneficiary", "country"].forEach((k) => {
    const v = f[k].split(",").map((s) => s.trim()).filter(Boolean);
    if (v.length) p.set(k, v.join(","));
  });
  ["minAmount", "maxAmount", "from", "to"].forEach((k) => f[k] && p.set(k, f[k]));
  ["riskLevel", "status"].forEach((k) => f[k].length && p.set(k, f[k].join(",")));
  p.set("sortBy", sort.by);
  p.set("sortDir", sort.dir);
  p.set("page", page);
  p.set("size", PAGE_SIZE);
  return p.toString();
}

function StatCard({ label, value }) {
  return (
    <div className="stat-card">
      <div className="card-label">{label}</div>
      <div className="card-value">{value}</div>
    </div>
  );
}

function FilterSidebar({ draft, setField, toggleIn, apply, reset }) {
  return (
    <aside className="filter-sidebar">
      <div className="filter-heading">
        <div>
          <span className="eyebrow">TRANSACTION SEARCH</span>
          <h2>Filters</h2>
        </div>
        <button type="button" className="text-button" onClick={reset}>Reset</button>
      </div>

      <form className="filter-form" onSubmit={apply}>
        <label>Customer IDs<input placeholder="C1023, C9231" value={draft.customerId} onChange={setField("customerId")} /></label>
        <label>Merchant<input placeholder="gold, apple" value={draft.merchant} onChange={setField("merchant")} /></label>
        <label>Beneficiary<input placeholder="Name / account" value={draft.beneficiary} onChange={setField("beneficiary")} /></label>
        <label>Country<input placeholder="India, UAE" value={draft.country} onChange={setField("country")} /></label>

        <div className="filter-grid">
          <label>Min amount<input type="number" placeholder="₹0" value={draft.minAmount} onChange={setField("minAmount")} /></label>
          <label>Max amount<input type="number" placeholder="₹0" value={draft.maxAmount} onChange={setField("maxAmount")} /></label>
        </div>

        <div className="filter-grid">
          <label>From<input type="date" value={draft.from} onChange={setField("from")} /></label>
          <label>To<input type="date" value={draft.to} onChange={setField("to")} /></label>
        </div>

        <fieldset>
          <legend>Risk level</legend>
          <div className="check-list">
            {RISK_LEVELS.map((l) => (
              <label key={l} className="check-row">
                <input type="checkbox" checked={draft.riskLevel.includes(l)} onChange={() => toggleIn("riskLevel", l)} />
                <span className={`risk-dot ${l.toLowerCase()}`} /> {l}
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend>Status</legend>
          <div className="check-list">
            {STATUSES.map((s) => (
              <label key={s} className="check-row">
                <input type="checkbox" checked={draft.status.includes(s)} onChange={() => toggleIn("status", s)} />
                {s}
              </label>
            ))}
          </div>
        </fieldset>

        <button className="primary-button" type="submit">Apply filters</button>
      </form>
    </aside>
  );
}

function TransactionDrawer({ transaction, onClose }) {
  if (!transaction) return null;

  return (
    <div className="drawer-backdrop" onMouseDown={onClose}>
      <aside className="transaction-drawer" onMouseDown={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <span className="eyebrow">TRANSACTION ANALYSIS</span>
            <h2>Transaction details</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Close transaction details">×</button>
        </div>

        <div className="drawer-scroll">
          <div className="risk-summary">
            <div>
              <span className="detail-label">Risk score</span>
              <strong className={`drawer-score ${transaction.riskLevel}`}>{transaction.riskScore}</strong>
            </div>
            <div>
              <span className="detail-label">Risk level</span>
              <strong className={`risk ${transaction.riskLevel}`}>{transaction.riskLevel}</strong>
            </div>
            <div>
              <span className="detail-label">Decision</span>
              <strong>{transaction.status}</strong>
            </div>
          </div>

          <section className="detail-section">
            <h3>Transaction</h3>
            <div className="detail-grid">
              <div><span className="detail-label">Amount</span><strong>{formatINR(transaction.amount)}</strong></div>
              <div><span className="detail-label">Date & time</span><strong>{new Date(transaction.createdAt).toLocaleString("en-IN")}</strong></div>
              <div><span className="detail-label">Location</span><strong>{transaction.location || "—"}</strong></div>
              <div><span className="detail-label">Country</span><strong>{transaction.country || "—"}</strong></div>
              <div><span className="detail-label">Merchant</span><strong>{transaction.merchant || "—"}</strong></div>
              <div><span className="detail-label">Merchant category</span><strong>{transaction.merchantCategory || "—"}</strong></div>
              <div><span className="detail-label">Beneficiary</span><strong>{transaction.beneficiary || "—"}</strong></div>
              <div><span className="detail-label">Device</span><strong>{transaction.deviceKey || "—"}</strong></div>
            </div>
          </section>

          <section className="detail-section">
            <h3>Customer</h3>
            <div className="detail-grid">
              <div><span className="detail-label">Customer</span><strong>{transaction.customerName}</strong></div>
              <div><span className="detail-label">Customer ID</span><strong>{transaction.customerId}</strong></div>
            </div>
          </section>

          <section className="detail-section">
            <div className="section-title-row">
              <h3>Why this transaction was flagged</h3>
              <span className="reason-count">{transaction.reasons?.length || 0} signals</span>
            </div>
            {transaction.reasons?.length ? (
              <div className="reason-list">
                {transaction.reasons.map((reason) => (
                  <article className="reason-card" key={`${reason.rule}-${reason.points}`}>
                    <div>
                      <strong>{reason.rule.replaceAll("_", " ")}</strong>
                      <p>{reason.description}</p>
                    </div>
                    <span className="points">+{reason.points}</span>
                  </article>
                ))}
              </div>
            ) : <p className="muted">No risk rules fired for this transaction.</p>}
          </section>
        </div>
      </aside>
    </div>
  );
}

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [draft, setDraft] = useState(EMPTY);
  const [applied, setApplied] = useState(EMPTY);
  const [sort, setSort] = useState({ by: "date", dir: "desc" });
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const hasLoaded = useRef(false);

  const load = useCallback(async ({ initial = false } = {}) => {
    if (initial) setLoading(true);
    else setRefreshing(true);

    const [dashboardResult, transactionsResult] = await Promise.allSettled([
      api("/dashboard"),
      api(`/transactions?${buildQuery(applied, sort, page)}`),
    ]);

    const errors = [];

    if (dashboardResult.status === "fulfilled") setStats(dashboardResult.value);
    else errors.push(`Dashboard: ${dashboardResult.reason?.message || "Unable to load statistics."}`);

    if (transactionsResult.status === "fulfilled") setResult(transactionsResult.value);
    else errors.push(`Transactions: ${transactionsResult.reason?.message || "Unable to load transactions."}`);

    setError(errors.join(" "));
    setLoading(false);
    hasLoaded.current = true;
    setRefreshing(false);
  }, [applied, sort, page]);

  useEffect(() => {
    load({ initial: !hasLoaded.current });
    const id = setInterval(() => load(), 10000);
    return () => clearInterval(id);
  }, [load]);

  const setField = (k) => (e) => setDraft({ ...draft, [k]: e.target.value });
  const toggleIn = (k, v) =>
    setDraft({ ...draft, [k]: draft[k].includes(v) ? draft[k].filter((x) => x !== v) : [...draft[k], v] });

  const apply = (e) => { e.preventDefault(); setPage(0); setApplied({ ...draft }); };
  const reset = () => { setDraft({ ...EMPTY, riskLevel: [], status: [] }); setApplied({ ...EMPTY, riskLevel: [], status: [] }); setPage(0); };

  const sortBy = (by) => {
    setPage(0);
    setSort((s) => (s.by === by ? { by, dir: s.dir === "asc" ? "desc" : "asc" } : { by, dir: "desc" }));
  };
  const Th = ({ id, children }) => {
    const active = sort.by === id;
    const direction = active ? sort.dir : null;
    return (
      <th scope="col" className="sortable">
        <button
          type="button"
          className="sort-button"
          onClick={() => sortBy(id)}
          aria-label={`Sort by ${children}${active ? `, currently ${direction === "asc" ? "ascending" : "descending"}` : ""}`}
        >
          <span>{children}</span>
          <span className={`sort-indicator${active ? " active" : ""}`} aria-hidden="true">
            {direction === "asc" ? "▲" : direction === "desc" ? "▼" : "↕"}
          </span>
        </button>
      </th>
    );
  };

  const viewTransaction = async (id) => {
    try {
      setSelected(await api(`/transactions/${id}`));
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="dashboard-shell">
      {error && (
        <div className="error banner-error" role="alert">
          <span>{error}</span>
          <button type="button" className="retry-button" onClick={() => load()}>Retry</button>
        </div>
      )}

      {loading ? (
        <div className="cards" aria-label="Loading dashboard statistics">
          {["Transaction volume", "Suspicious transactions", "High risk", "Blocked"].map((label) => (
            <div className="stat-card skeleton-card" key={label}>
              <div className="card-label">{label}</div>
              <div className="skeleton skeleton-value" />
            </div>
          ))}
        </div>
      ) : stats ? (
        <div className="cards">
          <StatCard label="Transaction volume" value={formatINR(stats.totalAmount)} />
          <StatCard label="Suspicious transactions" value={stats.suspiciousTransactions} />
          <StatCard label="High risk" value={stats.highRisk} />
          <StatCard label="Blocked" value={stats.blocked} />
        </div>
      ) : null}

      <div className="dashboard-layout">
        <FilterSidebar draft={draft} setField={setField} toggleIn={toggleIn} apply={apply} reset={reset} />

        <main className="transaction-panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">LIVE MONITORING</span>
              <h2>Transactions</h2>
              <p className="panel-subtitle">Review, sort and investigate incoming banking activity.</p>
            </div>
            <div className="panel-meta">
              {refreshing && <span className="refreshing" role="status">Updating…</span>}
              <span className="result-count">{loading ? "Loading results…" : `${result.totalElements} result(s)`}</span>
            </div>
          </div>

          {loading ? (
            <div className="table-wrap skeleton-table" aria-label="Loading transactions">
              <table>
                <tbody>
                  {Array.from({ length: 6 }).map((_, index) => (
                    <tr key={index}>
                      {Array.from({ length: 7 }).map((__, cell) => (
                        <td key={cell}><div className="skeleton skeleton-cell" /></td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <Th id="customer">Customer</Th>
                  <Th id="merchant">Merchant</Th>
                  <Th id="amount">Amount</Th>
                  <Th id="risk">Risk</Th>
                  <Th id="status">Status</Th>
                  <Th id="date">Time</Th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((t) => (
                  <tr key={t.id}>
                    <td>{t.customerId}</td>
                    <td>{t.merchant}</td>
                    <td>{formatINR(t.amount)}</td>
                    <td><span className={`risk ${t.riskLevel}`}>{t.riskScore}</span></td>
                    <td><span className={`status ${t.status}`}>{t.status}</span></td>
                    <td>{new Date(t.createdAt).toLocaleString("en-IN")}</td>
                    <td><button className="view-button" onClick={() => viewTransaction(t.id)}>View</button></td>
                  </tr>
                ))}
                {result.content.length === 0 && (
                  <tr><td colSpan={7} className="muted empty-state">No transactions match these filters.</td></tr>
                )}
              </tbody>
            </table>
          </div>
          )}

          <div className="pager">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
            <span>Page {result.totalPages === 0 ? 0 : page + 1} of {result.totalPages}</span>
            <button disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </main>
      </div>

      <TransactionDrawer transaction={selected} onClose={() => setSelected(null)} />
    </div>
  );
}
