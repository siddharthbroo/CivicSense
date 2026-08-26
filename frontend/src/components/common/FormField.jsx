/**
 * A labeled input with an optional helper note and inline error text.
 * Keeps forms consistent across the whole app instead of every page
 * hand-rolling label/input/error markup.
 */
export default function FormField({
  id,
  label,
  error,
  helperText,
  required = false,
  className = '',
  ...inputProps
}) {
  return (
    <div className={className}>
      <label htmlFor={id} className="field-label">
        {label}
        {required && <span className="ml-0.5 text-teal-700">*</span>}
      </label>
      <input
        id={id}
        className={`input-field ${error ? 'input-field-error' : ''}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        {...inputProps}
      />
      {error ? (
        <p id={`${id}-error`} className="field-error">
          {error}
        </p>
      ) : helperText ? (
        <p className="mt-1.5 text-xs text-slate-500">{helperText}</p>
      ) : null}
    </div>
  )
}
