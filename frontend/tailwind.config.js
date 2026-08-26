/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Primary — deep navy (authority, government-grade trust)
        navy: {
          50: '#eef1f6',
          100: '#d6dde8',
          200: '#adbbd1',
          300: '#8499ba',
          400: '#5b77a3',
          500: '#3d5a86',
          600: '#2a4468',
          700: '#1d3252',
          800: '#13233b',
          900: '#0b1526',
          950: '#060c17',
        },
        // Accent — teal (civic action, verified progress)
        teal: {
          50: '#eafbfa',
          100: '#cef4f2',
          200: '#9ee8e4',
          300: '#69d6d1',
          400: '#3abdb8',
          500: '#1f9d99',
          600: '#187d7a',
          700: '#166363',
          800: '#144f4f',
          900: '#0f3a3a',
        },
        // Neutral surfaces / secondary text
        slate: {
          25: '#f8f9fb',
          50: '#f4f5f8',
          100: '#e9ebf0',
          200: '#d3d7e0',
          300: '#aeb4c2',
          400: '#848c9e',
          500: '#636b7e',
          600: '#4a5164',
          700: '#383e4e',
          800: '#262b37',
          900: '#181b24',
        },
      },
      fontFamily: {
        sans: ['"Public Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['"Source Serif 4"', 'ui-serif', 'Georgia', 'serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(11, 21, 38, 0.06), 0 1px 3px rgba(11, 21, 38, 0.08)',
        elevated: '0 4px 12px rgba(11, 21, 38, 0.10), 0 2px 4px rgba(11, 21, 38, 0.06)',
      },
      borderRadius: {
        sm: '4px',
        DEFAULT: '6px',
        md: '8px',
        lg: '10px',
      },
    },
  },
  plugins: [],
}
