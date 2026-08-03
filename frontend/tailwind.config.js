/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  darkMode: 'class', // Enables toggle to dark mode via class insertion
  theme: {
    extend: {
      colors: {
        primary: '#005f73', // Deep Teal
        secondary: '#5e548e', // Slate Blue
        accent: '#2a9d8f', // Mint Green
        lightBg: '#F8FAFC', // Light Background
        darkBg: '#0F172A', // Dark Background[cite: 1]
        // Explicitly excluding any orange variables per project constraints
      },
    },
  },
  plugins: [],
}

