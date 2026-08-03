import React from "react";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import { ThemeContext } from "../context/ThemeContext";
import { NotesManager } from "../components/notes/NotesManager";

const HomePage = () => (
  <div className="flex flex-col items-center justify-center min-h-[80vh]">
    <h1 className="text-3xl font-bold text-primary mb-4">
      Welcome to CartMind
    </h1>
    <p className="mb-8">Your AI-powered grocery planning platform.</p>
    <Link
      to="/notes"
      className="bg-primary text-white px-6 py-2 rounded-lg shadow-md"
    >
      Get Started
    </Link>
  </div>
);

const LoginPage = () => (
  <div className="p-4 max-w-md mx-auto">
    <h2 className="text-2xl text-secondary font-bold mb-4">Login</h2>
    <p className="text-sm text-slate-600 dark:text-slate-400">
      Authentication flow active. Enter credentials to proceed.
    </p>
  </div>
);

export const AppRouter = () => {
  const { theme, toggleTheme } = React.useContext(ThemeContext);
  // Temporary token container until AuthContext state is passed
  const token = localStorage.getItem("cartmind_token") || "";

  return (
    <Router>
      <div className="min-h-screen bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 transition-colors duration-300 pb-16 md:pb-0 md:pl-64">
        <header className="p-4 flex justify-end">
          <button
            onClick={toggleTheme}
            className="p-2 bg-slate-200 dark:bg-slate-700 rounded-full shadow-sm text-xs font-medium"
          >
            Toggle to {theme === "light" ? "Dark" : "Light"} Mode
          </button>
        </header>

        <main className="container mx-auto px-4">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/notes" element={<NotesManager token={token} />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
};
