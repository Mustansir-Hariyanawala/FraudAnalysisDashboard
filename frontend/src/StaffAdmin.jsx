import { useEffect, useState } from "react";
import { api } from "./api";
import { fmtDateTime } from "./format";
const EMPTY = { username: "", fullName: "", email: "", password: "", role: "ANALYST" };

export default function StaffAdmin() {
  const [staff, setStaff] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [msg, setMsg] = useState("");

  const load = () => api("/admin/staff").then(setStaff).catch((e) => setMsg(e.message));
  useEffect(() => { load(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const create = async (e) => {
    e.preventDefault();
    try {
      await api("/admin/staff", { method: "POST", body: form });
      setForm(EMPTY); setMsg("Account created."); load();
    } catch (err) { setMsg(err.message); }
  };

  const toggle = async (s) => {
    try { await api(`/admin/staff/${s.id}/enabled?value=${!s.enabled}`, { method: "PATCH" }); load(); }
    catch (err) { setMsg(err.message); }
  };

  return (
    <div>
      <h2>Staff accounts</h2>
      {msg && <p className="error">{msg}</p>}
      <form className="filters" onSubmit={create}>
        <input placeholder="Username" value={form.username} onChange={set("username")} />
        <input placeholder="Full name" value={form.fullName} onChange={set("fullName")} />
        <input placeholder="Email" value={form.email} onChange={set("email")} />
        <input type="password" placeholder="Password (min 10)" value={form.password} onChange={set("password")} />
        <select value={form.role} onChange={set("role")}>
          <option>ANALYST</option><option>ADMIN</option>
        </select>
        <button type="submit">Create</button>
      </form>
      <table>
        <thead><tr><th>Username</th><th>Name</th><th>Email</th><th>Role</th><th>Last login</th><th>Status</th></tr></thead>
        <tbody>
          {staff.map((s) => (
            <tr key={s.id}>
              <td>{s.username}</td><td>{s.fullName}</td><td>{s.email}</td><td>{s.role}</td>
              <td>{s.lastLoginAt ? fmtDateTime(t.createdAt) : "—"}</td>
              <td><button onClick={() => toggle(s)}>{s.enabled ? "Disable" : "Enable"}</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}