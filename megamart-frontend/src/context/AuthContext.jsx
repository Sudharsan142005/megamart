import { createContext, useContext, useEffect, useState } from 'react';
import api, { dataOf } from '../services/api';
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); const [loading, setLoading] = useState(true);
  const refresh = async () => { try { const r = await api.get('/api/auth/me'); setUser(dataOf(r)); } catch { setUser(null); } finally { setLoading(false); } };
  useEffect(() => { refresh(); const clear = () => setUser(null); window.addEventListener('megamart:unauthorized', clear); return () => window.removeEventListener('megamart:unauthorized', clear); }, []);
  const login = async (username, password) => { const r = await api.post('/api/auth/login', { username, password }); const u = dataOf(r); setUser(u); return u; };
  const logout = async () => { try { await api.post('/api/auth/logout'); } finally { setUser(null); } };
  return <AuthContext.Provider value={{ user, loading, login, logout, refresh, setUser }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
