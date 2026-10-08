import { createContext, useContext, useEffect, useState } from "react";
import { api, setUnauthorizedHandler } from "./api";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
    api("/auth/me").then(setUser).catch(() => setUser(null)).finally(() => setLoading(false));
  }, []);

  const login = async (username, password) =>
    setUser(await api("/auth/login", { method: "POST", body: { username, password } }));

  const logout = async () => {
    await api("/auth/logout", { method: "POST" }).catch(() => {});
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>;
}