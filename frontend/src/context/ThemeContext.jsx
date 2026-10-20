import { createContext, useState, useEffect } from "react";
// import axios from 'axios'; // Will be used when backend is ready

// eslint-disable-next-line react-refresh/only-export-components
export const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState("light");

  useEffect(() => {
    const root = window.document.documentElement;
    root.classList.remove("light", "dark");
    root.classList.add(theme);

    // Mock API call to save user preference to the database[cite: 1]
    const saveThemePreference = async () => {
      try {
        console.log(
          `INFO: Attempting to save ${theme} theme preference to database.`,
        );
        // await axios.post('/api/user/preferences', { theme });
      } catch (error) {
        console.error("ERROR: Failed to save theme preference.", error);
      }
    };

    saveThemePreference();
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === "light" ? "dark" : "light"));
    console.log("INFO: User toggled theme.");
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
