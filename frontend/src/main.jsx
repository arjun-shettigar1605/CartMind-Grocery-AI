import React from "react";
import ReactDOM from "react-dom/client";
import { AppRouter } from "./routes/AppRouter.jsx";
import { ThemeProvider } from "./context/ThemeContext.jsx";
import "./index.css";
import { AuthProvider } from "./context/AuthContext.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AuthProvider>
      <ThemeProvider>
        <AppRouter />
      </ThemeProvider>
    </AuthProvider>
  </React.StrictMode>,
);
