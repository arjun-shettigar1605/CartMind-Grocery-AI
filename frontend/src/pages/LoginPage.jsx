import { useState } from "react";
import axios from "axios";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { API_BASE_URL } from "../config";


export const LoginPage = () => {
  const [isRegistering, setIsRegistering] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [status, setStatus] = useState({ type: "", message: "" });
  const [isLoading, setIsLoading] = useState(false);
  const { login, isAuthenticated } = useAuth();
  const location = useLocation();
  const redirectTo = location.state?.from || "/notes";


  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setStatus({ type: "", message: "" });

    try {
      const endpoint = isRegistering ? "/auth/register" : "/auth/login";
      const payload = isRegistering
        ? { email, password, fullName }
        : { email, password };

      const response = await axios.post(`${API_BASE_URL}${endpoint}`, payload, {
        withCredentials: true,
      });

      login(response.data); 
      
    } catch (err) {
      setStatus({
        type: "error",
        message:
          err.response?.data?.message ||
          "Authentication failed. Check credentials.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  if (isAuthenticated) return <Navigate to={redirectTo} replace />;

  return (
    <div className="max-w-md mx-auto p-6 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-100 dark:border-slate-700 mt-12">
      <h2 className="text-2xl font-bold text-primary dark:text-accent mb-2">
        {isRegistering ? "Create Account" : "Account Login"}
      </h2>
      <p className="text-sm text-slate-600 dark:text-slate-400 mb-6">
        Authenticate your session to access AI notes and sync with Swiggy
        Instamart.
      </p>

      <form onSubmit={handleSubmit} className="space-y-4">
        {isRegistering && (
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Full Name
            </label>
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              required
              className="w-full p-2.5 rounded-lg border border-slate-300 dark:border-slate-600 bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 focus:ring-2 focus:ring-accent outline-none"
            />
          </div>
        )}
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
            Email Address
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full p-2.5 rounded-lg border border-slate-300 dark:border-slate-600 bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 focus:ring-2 focus:ring-accent outline-none"
          />
        </div>
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full p-2.5 rounded-lg border border-slate-300 dark:border-slate-600 bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 focus:ring-2 focus:ring-accent outline-none"
          />
        </div>

        {status.message && (
          <p
            className={`text-xs font-semibold ${status.type === "error" ? "text-red-500" : "text-accent"}`}
          >
            {status.message}
          </p>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className="w-full py-2.5 bg-primary hover:bg-opacity-90 dark:bg-accent text-white font-bold rounded-lg shadow-sm transition"
        >
          {isLoading
            ? "Authenticating..."
            : isRegistering
              ? "Register"
              : "Login"}
        </button>
      </form>

      <div className="mt-6 text-center">
        <button
          onClick={() => setIsRegistering(!isRegistering)}
          className="text-xs text-secondary dark:text-slate-300 hover:underline"
        >
          {isRegistering
            ? "Already have an account? Login here."
            : "Need an account? Register here."}
        </button>
      </div>
    </div>
  );
};
