import { useCallback, useEffect, useState } from "react";
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
    <div className="card">
      <div className="card-label">{label}</div>
      <div className="card-value">{value}</div>
    </div>
  );
}

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [draft, setDraft] = useState(EMPTY);      // what the user is typing
  const [applied, setApplied] = useState(EMPTY);  // what was last submitted
  const [sort, setSort] = useState({ by: "date", dir: "desc" });
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      const [d, t] = await Promise.all([
        api("/dashboard"),
        api(`/transactions?${buildQuery(applied, sort, page)}`),
      ]);
      setStats(d); setResult(t); setError("");
    } catch (e) { setError(e.message); }
  }, [applied, sort, page]);

  useEffect(() => {
    load();
    const id = setInterval(load, 10000);
    return () => clearInterval(id);
  }, [load]);

  const setField = (k) => (e) => setDraft({ ...draft, [k]: e.target.value });
  const toggleIn = (k, v) =>
    setDraft({ ...draft, [k]: draft[k].includes(v) ? draft[k].filter((x) => x !== v) : [...draft[k], v] });

  const apply = (e) => { e.preventDefault(); setPage(0); setApplied(draft); };
  const reset = () => { setDraft(EMPTY); setApplied(EMPTY); setPage(0); };

  const sortBy = (by) => {
    setPage(0);
    setSort((s) => (s.by === by ? { by, dir: s.dir === "asc" ? "desc" : "asc" } : { by, dir: "desc" }));
  };
  const Th = ({ id, children }) => (
    <th className="sortable" onClick={() => sortBy(id)}>
      {children} {sort.by === id ? (sort.dir === "asc" ? "▲" : "▼") : ""}
    </th>
  );

  const viewTransaction = async (id) => setSelected(await api(`/transactions/${id}`));

  return (
    <div>
      {error && <p className="error">{error}</p>}

      {stats && (
        <div className="cards">
          <StatCard label="Total Transactions" value={formatINR(stats.totalAmount)} />
          <StatCard label="Suspicious Transactions" value={stats.suspiciousTransactions} />
          <StatCard label="High Risk" value={stats.highRisk} />
          <StatCard label="Blocked" value={stats.blocked} />
        </div>
      )}

      <form className="filters" onSubmit={apply}>
        <input placeholder="Customer IDs (C1023, C9231)" value={draft.customerId} onChange={setField("customerId")} />
        <input placeholder="Merchants (gold, apple)" value={draft.merchant} onChange={setField("merchant")} />
        <input placeholder="Beneficiary name / account" value={draft.beneficiary} onChange={setField("beneficiary")} />
        <input placeholder="Countries (India, UAE)" value={draft.country} onChange={setField("country")} />
        <input type="number" placeholder="Min amount ₹" value={draft.minAmount} onChange={setField("minAmount")} />
        <input type="number" placeholder="Max amount ₹" value={draft.maxAmount} onChange={setField("maxAmount")} />
        <label>From <input type="date" value={draft.from} onChange={setField("from")} /></label>
        <label>To <input type="date" value={draft.to} onChange={setField("to")} /></label>

        <fieldset>
          <legend>Risk level</legend>
          {RISK_LEVELS.map((l) => (
            <label key={l}>
              <input type="checkbox" checked={draft.riskLevel.includes(l)} onChange={() => toggleIn("riskLevel", l)} /> {l}
            </label>
          ))}
        </fieldset>
        <fieldset>
          <legend>Status</legend>
          {STATUSES.map((s) => (
            <label key={s}>
              <input type="checkbox" checked={draft.status.includes(s)} onChange={() => toggleIn("status", s)} /> {s}
            </label>
          ))}
        </fieldset>

        <button type="submit">Apply filters</button>
        <button type="button" onClick={reset}>Reset</button>
      </form>

      <p className="muted">{result.totalElements} result(s)</p>

      <table>
        <thead>
          <tr>
            <th>Transaction ID</th>
            <Th id="customer">Customer</Th>
            <Th id="merchant">Merchant</Th>
            <Th id="amount">Amount</Th>
            <Th id="risk">Risk</Th>
            <Th id="status">Status</Th>
            <Th id="date">Time</Th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {result.content.map((t) => (
            <tr key={t.id}>
              <td>{t.transactionId}</td>
              <td>{t.customerId}</td>
              <td>{t.merchant}</td>
              <td>{formatINR(t.amount)}</td>
              <td><span className={`risk ${t.riskLevel}`}>{t.riskScore}</span></td>
              <td>{t.status}</td>
              <td>{new Date(t.createdAt).toLocaleString("en-IN")}</td>
              <td><button onClick={() => viewTransaction(t.id)}>View Transaction</button></td>
            </tr>
          ))}
          {result.content.length === 0 && (
            <tr><td colSpan={8} className="muted">No transactions match these filters.</td></tr>
          )}
        </tbody>
      </table>

      <div className="pager">
        <button disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
        <span>Page {result.totalPages === 0 ? 0 : page + 1} of {result.totalPages}</span>
        <button disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>

      {/* paste the existing {selected && (<div className="modal-bg"> ... )} block from before, unchanged */}
    </div>
  );
}