import { useRef, useState } from 'react'

const ACCEPTED_TYPES = '.jpg,.jpeg,.png,.pdf'
const MAX_SIZE_MB = 10

/**
 * A click-or-drag file picker for the identity document. Purely a UI
 * concern — validation here is limited to file presence/size/type; the
 * backend performs the actual OCR and any authoritative validation.
 */
export default function DocumentDropzone({ file, onFileSelect, error, disabled }) {
  const inputRef = useRef(null)
  const [isDragging, setIsDragging] = useState(false)

  function validateAndSet(selected) {
    if (!selected) return
    if (selected.size > MAX_SIZE_MB * 1024 * 1024) {
      onFileSelect(null, `File is too large. Maximum size is ${MAX_SIZE_MB}MB.`)
      return
    }
    onFileSelect(selected, null)
  }

  function handleDrop(e) {
    e.preventDefault()
    setIsDragging(false)
    if (disabled) return
    validateAndSet(e.dataTransfer.files?.[0])
  }

  return (
    <div>
      <span className="field-label">
        Identity document
        <span className="ml-0.5 text-teal-700">*</span>
      </span>
      <div
        onDragOver={(e) => {
          e.preventDefault()
          if (!disabled) setIsDragging(true)
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => !disabled && inputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if ((e.key === 'Enter' || e.key === ' ') && !disabled) inputRef.current?.click()
        }}
        className={[
          'flex cursor-pointer flex-col items-center justify-center rounded-md border-2 border-dashed px-6 py-8 text-center transition-colors',
          disabled && 'cursor-not-allowed opacity-60',
          isDragging ? 'border-teal-500 bg-teal-50' : 'border-slate-300 bg-slate-25 hover:border-teal-400',
          error && 'border-red-400',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES}
          className="sr-only"
          disabled={disabled}
          onChange={(e) => validateAndSet(e.target.files?.[0])}
        />
        <UploadIcon />
        {file ? (
          <div className="mt-3">
            <p className="text-sm font-medium text-navy-800">{file.name}</p>
            <p className="mt-0.5 text-xs text-slate-500">{(file.size / (1024 * 1024)).toFixed(2)} MB · click to replace</p>
          </div>
        ) : (
          <div className="mt-3">
            <p className="text-sm font-medium text-navy-800">
              <span className="text-teal-700">Click to upload</span> or drag and drop
            </p>
            <p className="mt-0.5 text-xs text-slate-500">JPG, PNG, or PDF · up to {MAX_SIZE_MB}MB</p>
          </div>
        )}
      </div>
      {error && <p className="field-error">{error}</p>}
    </div>
  )
}

function UploadIcon() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" className="text-teal-600" aria-hidden="true">
      <path
        d="M12 16V4m0 0L7 9m5-5l5 5M5 20h14"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
