const STEPS = [
  { key: 'identity', label: 'Identity' },
  { key: 'review', label: 'Review' },
  { key: 'mobile', label: 'Mobile' },
  { key: 'otp', label: 'Verify' },
  { key: 'password', label: 'Password' },
]

/**
 * @param {{ currentStep: 'identity'|'review'|'mobile'|'otp'|'password' }} props
 */
export default function StepIndicator({ currentStep }) {
  const currentIndex = STEPS.findIndex((s) => s.key === currentStep)

  return (
    <ol className="mb-8 flex items-center justify-between" aria-label="Registration progress">
      {STEPS.map((step, index) => {
        const isComplete = index < currentIndex
        const isCurrent = index === currentIndex

        return (
          <li key={step.key} className="flex flex-1 items-center last:flex-none">
            <div className="flex flex-col items-center gap-1.5">
              <span
                className={[
                  'flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold',
                  isComplete && 'bg-teal-600 text-white',
                  isCurrent && !isComplete && 'bg-navy-800 text-white',
                  !isComplete && !isCurrent && 'bg-slate-100 text-slate-400',
                ]
                  .filter(Boolean)
                  .join(' ')}
                aria-current={isCurrent ? 'step' : undefined}
              >
                {isComplete ? '✓' : index + 1}
              </span>
              <span
                className={`text-[11px] font-medium ${
                  isCurrent ? 'text-navy-800' : 'text-slate-400'
                }`}
              >
                {step.label}
              </span>
            </div>
            {index < STEPS.length - 1 && (
              <span
                className={`mx-2 h-px flex-1 ${isComplete ? 'bg-teal-600' : 'bg-slate-200'}`}
                aria-hidden="true"
              />
            )}
          </li>
        )
      })}
    </ol>
  )
}
