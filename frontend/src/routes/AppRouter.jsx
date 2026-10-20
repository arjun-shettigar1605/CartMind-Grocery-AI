import React from "react";
// import { useContext } from "react";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import { ThemeContext } from "../context/ThemeContext";
import { NotesManager } from "../components/notes/NotesManager";
import { Navigation } from "../components/layout/Navigation";
import { LoginPage } from "../pages/LoginPage";
import { useAuth } from "../context/AuthContext";
import { ProtectedRoute } from "../components/auth/ProtectedRoute";



const HomePage = () => (
  <div className="flex flex-col items-center justify-center min-h-[70vh] text-center px-4">
    <h1 className="text-4xl font-extrabold text-primary dark:text-accent mb-4 tracking-tight">
      AI-Powered Grocery Planning
    </h1>
    <p className="max-w-xl text-slate-600 dark:text-slate-400 mb-8 text-base">
      Convert natural language notes into shopping carts using Gemini structured
      output, and Swiggy Instamart MCP integration.
    </p>
    <Link
      to="/notes"
      className="bg-primary hover:bg-opacity-90 text-white font-semibold px-8 py-3 rounded-xl shadow-md transition"
    >
      Open Grocery Notes
    </Link>
  </div>
);


export const AppRouter = () => {
  const { theme, toggleTheme } = React.useContext(ThemeContext);
  // Temporary token container until AuthContext state is passed
  const token = localStorage.getItem("cartmind_token") || "";

  const { user, isAuthenticated, logout } = useAuth();


  return (
    <Router>
      <div className="min-h-screen bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 transition-colors duration-300">
        <Navigation />
        <div className="md:pl-64 flex flex-col min-h-screen">
          <header className="p-4 flex justify-end border-b border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-darkBg/50 backdrop-blur-sm">
            {isAuthenticated && (
              <span className="text-xs text-slate-600 dark:text-slate-400">
                {user?.fullName}
              </span>
            )}
            {isAuthenticated && (
              <button
                onClick={logout}
                className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 rounded-lg text-xs font-medium"
              >
                Logout
              </button>
            )}

            <button
              onClick={toggleTheme}
              className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-medium transition"
            >
              Toggle to {theme === "light" ? "Dark" : "Light"} Mode
            </button>
          </header>

          <main className="flex-1 p-4 md:p-8">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/notes" element={<ProtectedRoute><NotesManager token={token} /></ProtectedRoute>} />
            </Routes>
          </main>
        </div>
      </div>
    </Router>
  );
};
