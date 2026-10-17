/**
 * CivicGuidelines - Citizen handbook and emergency helplines.
 * Mobile-first with direct tap-to-call functionality.
 */
export default function CivicGuidelines() {
  const steps = [
    {
      num: '01',
      title: 'Photo & GPS Capture',
      desc: 'Take a clear photograph of the hazard with your device location enabled.',
      icon: '📸',
      color: 'bg-teal-50 text-teal-800 border-teal-200',
    },
    {
      num: '02',
      title: 'Automated Dispatch',
      desc: 'CivicSense indexes the report and routes it straight to your ward municipal engineer.',
      icon: '🏛️',
      color: 'bg-navy-50 text-navy-800 border-navy-200',
    },
    {
      num: '03',
      title: 'Field Team In Action',
      desc: 'Ground crews are mobilized to the coordinates to perform repairs or cleanups.',
      icon: '🛠️',
      color: 'bg-blue-50 text-blue-800 border-blue-200',
    },
    {
      num: '04',
      title: 'Verified Resolution',
      desc: 'Once the fix is validated on-site, the complaint is marked resolved with status updates.',
      icon: '✅',
      color: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    },
  ]

  const dos = [
    'Enable GPS location permission so inspection teams reach the exact spot.',
    'Take photos in daylight showing both the damage and nearby recognizable landmarks.',
    'Mention specific street names, gate numbers, or public landmarks.',
  ]

  const donts = [
    'Do not upload blurred, downloaded, or unrelated photos.',
    'Do not submit duplicate reports for the same issue in under 24 hours.',
    'For life-threatening accidents, call 112 emergency services immediately.',
  ]

  const emergencyContacts = [
    { name: 'Municipal Citizen Line', number: '1913', desc: 'Garbage, sanitation, drains', tel: '1913' },
    { name: 'National Emergency', number: '112', desc: 'Police & all emergencies', tel: '112' },
    { name: 'Fire Service', number: '101', desc: 'Fire hazards & disasters', tel: '101' },
    { name: 'Ambulance Emergency', number: '108', desc: 'Medical emergencies', tel: '108' },
  ]

  return (
    <div className="space-y-6">
      {/* Workflow Section */}
      <div className="card overflow-hidden p-5 sm:p-7 border border-slate-200">
        <div className="flex items-center gap-2.5">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-teal-100 text-teal-800 text-sm">
            ⚙️
          </span>
          <div>
            <h3 className="font-display text-base sm:text-lg font-bold text-navy-900">
              How CivicSense Resolves Your Reports
            </h3>
            <p className="text-xs text-slate-500">
              A transparent, government-grade workflow connecting citizens to municipal engineers.
            </p>
          </div>
        </div>

        <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((s) => (
            <div
              key={s.num}
              className="relative flex flex-col justify-between rounded-xl border border-slate-200/80 bg-slate-50/70 p-4 transition-all hover:bg-white hover:shadow-sm"
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-2xl">{s.icon}</span>
                  <span className="font-mono text-xs font-bold text-slate-400">
                    STEP {s.num}
                  </span>
                </div>
                <h4 className="mt-3 text-xs sm:text-sm font-bold text-navy-900">
                  {s.title}
                </h4>
                <p className="mt-1 text-xs text-slate-600 leading-relaxed">
                  {s.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Dos & Don'ts */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="card p-5 sm:p-6 border border-slate-200">
          <h4 className="flex items-center gap-2 font-display text-sm font-bold text-green-900">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-green-100 text-green-700 text-xs">✓</span>
            <span>Best Practices (Do's)</span>
          </h4>
          <ul className="mt-3.5 space-y-2.5">
            {dos.map((item, idx) => (
              <li key={idx} className="flex items-start gap-2.5 text-xs text-slate-700 leading-relaxed">
                <span className="text-green-600 font-bold mt-0.5">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="card p-5 sm:p-6 border border-slate-200">
          <h4 className="flex items-center gap-2 font-display text-sm font-bold text-amber-900">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-amber-100 text-amber-700 text-xs">✕</span>
            <span>Important Guidelines (Don'ts)</span>
          </h4>
          <ul className="mt-3.5 space-y-2.5">
            {donts.map((item, idx) => (
              <li key={idx} className="flex items-start gap-2.5 text-xs text-slate-700 leading-relaxed">
                <span className="text-amber-600 font-bold mt-0.5">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Emergency Helpline Section */}
      <div className="card overflow-hidden p-5 sm:p-6 border border-slate-200">
        <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-3">
          <div>
            <h4 className="font-display text-sm font-bold text-navy-900 flex items-center gap-2">
              <span>🚨</span> Emergency Helplines
            </h4>
            <p className="text-[11px] text-slate-500">Tap any number on mobile to dial directly.</p>
          </div>
          <span className="rounded-full bg-red-50 px-2 py-0.5 text-[10px] font-bold text-red-700 ring-1 ring-inset ring-red-200">
            24x7 Toll-Free
          </span>
        </div>

        <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {emergencyContacts.map((contact) => (
            <a
              key={contact.name}
              href={`tel:${contact.tel}`}
              className="group flex flex-col justify-between rounded-xl border border-slate-200 bg-slate-50/60 p-3.5 text-center transition-all hover:border-teal-400 hover:bg-teal-50/40 active:scale-95 touch-target"
            >
              <div>
                <span className="block text-[11px] font-medium text-slate-600 truncate">
                  {contact.name}
                </span>
                <span className="mt-1 block font-mono text-lg font-bold text-navy-900 group-hover:text-teal-800">
                  {contact.number}
                </span>
              </div>
              <span className="mt-2 block text-[10px] text-teal-700 font-semibold group-hover:underline">
                Tap to Call &rarr;
              </span>
            </a>
          ))}
        </div>
      </div>
    </div>
  )
}
