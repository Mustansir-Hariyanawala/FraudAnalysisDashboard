import { useState } from "react";
import { AuthProvider, useAuth } from "./AuthContext";
import Login from "./Login";
import Dashboard from "./Dashboard";
import StaffAdmin from "./StaffAdmin";
import "./App.css";

function Shell() {
  const { user, loading, logout } = useAuth();
  const [view, setView] = useState("dashboard");

  if (loading) return <p className="muted">Loading…</p>;
  if (!user) return <Login />;

  return (
    <div className="container">
      <div className="topbar">
        <h1>FRAUD MONITORING DASHBOARD</h1>
        <div>
          <span>{user.fullName} ({user.role})</span>{" "}
          {user.role === "ADMIN" && (
            <button onClick={() => setView(view === "staff" ? "dashboard" : "staff")}>
              {view === "staff" ? "Dashboard" : "Manage staff"}
            </button>
          )}{" "}
          <button onClick={logout}>Logout</button>
        </div>
      </div>
      {view === "staff" && user.role === "ADMIN" ? <StaffAdmin /> : <Dashboard />}
    </div>
  );
}

export default function App() {
  return <AuthProvider><Shell /></AuthProvider>;
}