import { useAuth } from '../../context/AuthContext.jsx'

const ROLE_CONTENT = {
  CITIZEN: {
    heading: 'Citizen dashboard',
    description: 'File new complaints and track the ones you have already reported.',
    cards: [
      { title: 'File a complaint', body: 'Report a civic issue in your zone.' },
      { title: 'My complaints', body: 'Track the status of complaints you have submitted.' },
    ],
  },
  OFFICER: {
    heading: 'Officer dashboard',
    description: 'Review complaints assigned to you and update their status.',
    cards: [
      { title: 'Assigned complaints', body: 'Complaints routed to you for action.' },
      { title: 'Update status', body: 'Mark progress on complaints you are handling.' },
    ],
  },
  ADMIN: {
    heading: 'Admin dashboard',
    description: 'Manage users, assign complaints, and oversee the platform.',
    cards: [
      { title: 'User management', body: 'View and manage citizen and officer accounts.' },
      { title: 'Complaint assignment', body: 'Route incoming complaints to the right officer.' },
    ],
  },
}

/**
 * A single dashboard component that adapts its content by role. This
 * is a UI-level convenience only; no complaint-management APIs exist
 * yet, so the cards below are placeholders rather than live features.
 */
export default function Dashboard() {
  const { user, role } = useAuth()
  const content = ROLE_CONTENT[role] || ROLE_CONTENT.CITIZEN

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-semibold">Welcome back, {user?.name}</h1>
        <p className="mt-1 text-sm text-slate-500">{content.description}</p>
      </div>

      <div className="mb-8 grid gap-4 sm:grid-cols-2">
        {content.cards.map((card) => (
          <div key={card.title} className="card p-5">
            <h2 className="text-base font-semibold text-navy-800">{card.title}</h2>
            <p className="mt-1.5 text-sm text-slate-500">{card.body}</p>
            <span className="badge-pending mt-4 inline-block">Coming soon</span>
          </div>
        ))}
      </div>

      <div className="card p-5">
        <h2 className="text-base font-semibold text-navy-800">{content.heading}</h2>
        <p className="mt-1.5 text-sm text-slate-500">
          This dashboard will populate once the corresponding complaint-management APIs are
          available from the backend. The account layer — login, registration, JWT handling,
          and role-based routing — is fully wired end-to-end.
        </p>
      </div>
    </div>
  )
}
