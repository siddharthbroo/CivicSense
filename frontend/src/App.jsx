import AppRoutes from './routes/AppRoutes.jsx'

/**
 * App is intentionally thin. It does not contain page markup, API calls,
 * or business logic — it only mounts the route table. All real UI lives
 * under src/pages and src/components.
 */
export default function App() {
  return <AppRoutes />
}
