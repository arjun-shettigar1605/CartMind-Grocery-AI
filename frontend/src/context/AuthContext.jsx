import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import axios from "axios";
import { API_BASE_URL } from "../config";

const TOKEN_KEY = "cartmind_token";
const USER_KEY = "cartmind_user";

const isExpired = (token) => {
  try {
    const payload = JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")),
    );
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
};

const readStoredSession = () => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token || isExpired(token)) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return { token: "", user: null };
  }
  try {
    return { token, user: JSON.parse(localStorage.getItem(USER_KEY)) };
  } catch {
    return { token, user: null };
  }
};
// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext(null);
// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext);


export const AuthProvider = ({ children }) => {
  const [session, setSession] = useState(readStoredSession);

  const login = useCallback((authResponse) => {
    const { token, ...user } = authResponse; // user = { id, email, fullName, themePreference }
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    setSession({ token, user });
  }, []);

  const logout = useCallback(() => {
    axios
      .post(`${API_BASE_URL}/auth/logout`, {}, { withCredentials: true })
      .catch(() => {});
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setSession({ token: "", user: null });
  }, []);

  const updateUser = useCallback((changes) => {
    setSession((prev) => {
      const user = { ...prev.user, ...changes };
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      return { ...prev, user };
    });
  }, []);

  // Any 403 while logged in means the JWT is no longer valid (expired or wrong secret).
  useEffect(() => {
    const id = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 403 && localStorage.getItem(TOKEN_KEY))
          logout();
        return Promise.reject(error);
      },
    );
    return () => axios.interceptors.response.eject(id);
  }, [logout]);

  const value = useMemo(
    () => ({
      token: session.token,
      user: session.user,
      isAuthenticated: !!session.token,
      login,
      logout,
      updateUser,
    }),
    [session, login, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
