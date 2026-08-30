export default function CivicGuidelines() {
  const steps = [
    {
      num: '1',
      title: 'Report with Photo & GPS',
      desc: 'Capture a clear photo of the civic hazard and provide the exact location coordinates.',
      icon: '📸',
    },
    {
      num: '2',
      title: 'Automated Routing',
      desc: 'CivicSense classifies your report and immediately routes it to the designated municipal ward officer.',
      icon: '🏛️',
    },
    {
      num: '3',
      title: 'Field Action & Progress',
      desc: 'Municipal ground crews are dispatched to inspect and perform maintenance or repairs.',
      icon: '🛠️',
    },
    {
      num: '4',
      title: 'Resolution & Confirmation',
      desc: 'The complaint is marked resolved once verified with on-site inspection and proof.',
      icon: '✅',
    },
  ]

  const tips = [
    'Always allow GPS location permission so ground teams can navigate directly to the issue.',
    'Take photographs during daylight with recognizable surrounding landmarks when possible.',
    'Avoid uploading unrelated images or personal documents in civic issue reports.',
    'For urgent life-threatening emergencies, contact municipal emergency hotlines directly.',
  ]

  const emergencyContacts = [
    { name: 'Municipal Helpline', number: '1913' },
    { name: 'Police Helpline', number: '112' },
    { name: 'Fire & Emergency', number: '101' },
    { name: 'Ambulance Services', number: '108' },
  ]

  return (
    <div className="space-y-6">
      {/* Workflow Section */}
      <div className="card p-6 sm:p-8">
        <h3 className="font-display text-lg font-bold text-navy-900">How CivicSense Resolves Issues</h3>
        <p className="mt-1 text-xs text-slate-500">
          A transparent, government-grade platform connecting citizens directly with municipal departments.
        </p>

        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((s) => (
            <div key={s.num} className="rounded-lg border border-slate-100 bg-slate-50 p-4 transition-all hover:bg-white hover:shadow-sm">
              <div className="flex items-center gap-2">
                <span className="text-xl">{s.icon}</span>
                <span className="rounded-full bg-navy-100 px-2 py-0.5 text-[10px] font-bold text-navy-800">
                  Step {s.num}
                </span>
              </div>
              <h4 className="mt-2 text-sm font-semibold text-navy-900">{s.title}</h4>
              <p className="mt-1 text-xs leading-relaxed text-slate-600">{s.desc}</p>
            </div>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {/* Best Practice Tips */}
        <div className="card p-6">
          <h3 className="flex items-center gap-2 font-display text-base font-bold text-navy-900">
            <span className="text-teal-600">💡</span> Guidelines for Fast Resolution
          </h3>
          <ul className="mt-4 space-y-2.5">
            {tips.map((tip, idx) => (
              <li key={idx} className="flex items-start gap-2 text-xs leading-relaxed text-slate-700">
                <span className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-teal-100 text-[10px] font-bold text-teal-800">
                  ✓
                </span>
                <span>{tip}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Emergency Contacts */}
        <div className="card p-6">
          <h3 className="flex items-center gap-2 font-display text-base font-bold text-navy-900">
            <span className="text-red-600">🚨</span> Emergency Helplines
          </h3>
          <p className="mt-1 text-xs text-slate-500">
            For critical, immediate hazard emergencies in your municipal jurisdiction:
          </p>

          <div className="mt-4 grid grid-cols-2 gap-3">
            {emergencyContacts.map((contact) => (
              <div key={contact.name} className="rounded-md border border-slate-200 bg-slate-50 p-3 text-center">
                <span className="block text-[11px] font-medium text-slate-500">{contact.name}</span>
                <span className="mt-0.5 block font-mono text-base font-bold text-navy-900">{contact.number}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

