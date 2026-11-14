import { createContext, useState, useEffect } from "react";
import axios from 'axios'; 
import { API_BASE_URL } from "../config";
import { useAuth } from "./AuthContext";


// eslint-disable-next-line react-refresh/only-export-components
export const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
  const { token, user, updateUser } = useAuth();
  const [guestTheme, setGuestTheme] = useState(
    () => localStorage.getItem("cartmind_theme") || "light",
  );
  const theme = user?.themePreference || guestTheme;


  useEffect(() => {
    const root = window.document.documentElement;
    root.classList.remove("light", "dark");
    root.classList.add(theme);
  }, [theme]);

  const toggleTheme = async () => {
    const next = theme === "light" ? "dark" : "light";
    if (!user) {
      localStorage.setItem("cartmind_theme", next);
      setGuestTheme(next);
      return;
    }
    updateUser({ themePreference: next }); // update the UI immediately
    try {
      await axios.put(
        `${API_BASE_URL}/user/preferences`,
        { theme: next },
        { headers: { Authorization: `Bearer ${token}` } },
      );
    } catch (error) {
      console.error("Frontend log: Failed to save theme preference.", error);
      updateUser({ themePreference: theme }); // roll back if the save failed
    }
  };


  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
