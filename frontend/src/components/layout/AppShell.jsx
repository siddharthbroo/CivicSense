import { Outlet } from 'react-router-dom'
import Navbar from './Navbar.jsx'

/**
 * Layout for every authenticated screen: navbar + content area.
 * Rendered as a parent route so nested pages only need to define
 * their own content via <Outlet />.
 */
export default function AppShell() {
  return (
    <div className="min-h-screen bg-slate-25">
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  )
}
