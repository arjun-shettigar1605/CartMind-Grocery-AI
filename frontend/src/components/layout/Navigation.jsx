import { NavLink } from "react-router-dom";

const navItems = [
  { name: "Home", path: "/" },
  { name: "Notes", path: "/notes" },
  { name: "Login", path: "/login" },
];

export const Navigation = () => {
  const activeClass =
    "text-primary dark:text-accent font-semibold border-b-2 md:border-b-0 md:border-l-4 border-accent";
  const inactiveClass =
    "text-slate-600 dark:text-slate-400 hover:text-primary dark:hover:text-accent";

  return (
    <>
      {/* Desktop Sidebar */}
      <aside className="hidden md:flex flex-col w-64 fixed inset-y-0 left-0 bg-white dark:bg-darkBg border-r border-slate-200 dark:border-slate-800 z-30 p-6">
        <div className="flex items-center gap-2 mb-8">
          <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white font-bold">
            CM
          </div>
          <span className="text-xl font-bold text-primary dark:text-accent tracking-tight">
            CartMind
          </span>
        </div>

        <nav className="space-y-2 flex-1">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center px-4 py-2.5 rounded-lg text-sm transition-colors ${
                  isActive ? activeClass : inactiveClass
                }`
              }
            >
              {item.name}
            </NavLink>
          ))}
        </nav>

        <div className="pt-4 border-t border-slate-200 dark:border-slate-800 text-xs text-slate-500">
          CartMind v1.0 • Stage 3
        </div>
      </aside>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 bg-white dark:bg-darkBg border-t border-slate-200 dark:border-slate-800 flex justify-around items-center py-2 z-30 shadow-lg">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex flex-col items-center py-1 px-3 text-xs ${isActive ? activeClass : inactiveClass}`
            }
          >
            {item.name}
          </NavLink>
        ))}
      </nav>
    </>
  );
};
