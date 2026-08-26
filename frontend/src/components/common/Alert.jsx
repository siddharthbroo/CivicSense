const VARIANT_STYLES = {
  error: 'bg-red-50 border-red-200 text-red-700',
  success: 'bg-green-50 border-green-200 text-green-700',
  info: 'bg-teal-50 border-teal-200 text-teal-800',
  warning: 'bg-amber-50 border-amber-200 text-amber-800',
}

/**
 * A single-purpose banner for surfacing API errors, success messages,
 * or informational notes. Never renders raw JS error objects — callers
 * pass a plain, user-facing string.
 */
export default function Alert({ variant = 'info', title, children, className = '' }) {
  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={`rounded-md border px-4 py-3 text-sm ${VARIANT_STYLES[variant]} ${className}`}
    >
      {title && <p className="font-semibold">{title}</p>}
      {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
    </div>
  )
}
