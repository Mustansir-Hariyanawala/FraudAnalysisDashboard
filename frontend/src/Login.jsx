import { useState } from "react";
import { useAuth } from "./AuthContext";

export default function Login() {
  const { login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    try { await login(username, password); }
    catch (err) { setError(err.message); }
  };

  return (
    <form className="login" onSubmit={submit}>
      <h2>Fraud Monitoring Login</h2>
      {error && <p className="error">{error}</p>}
      <input placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
      <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button type="submit">Sign in</button>
    </form>
  );
}