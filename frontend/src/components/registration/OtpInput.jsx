import { useEffect, useRef } from 'react'

const OTP_LENGTH = 6

/**
 * Six-box OTP entry. Keeps a single string value in the parent and
 * handles per-box focus movement, backspace, and paste.
 */
export default function OtpInput({ value, onChange, disabled, error }) {
  const inputRefs = useRef([])
  const digits = value.padEnd(OTP_LENGTH, ' ').split('').slice(0, OTP_LENGTH)

  useEffect(() => {
    inputRefs.current[0]?.focus()
  }, [])

  function setDigitAt(index, digit) {
    const next = [...digits]
    next[index] = digit
    onChange(next.join('').trimEnd())
  }

  function handleChange(index, rawValue) {
    const digit = rawValue.replace(/\D/g, '').slice(-1)
    setDigitAt(index, digit || ' ')
    if (digit && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  function handleKeyDown(index, e) {
    if (e.key === 'Backspace' && !digits[index].trim() && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  function handlePaste(e) {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH)
    if (pasted) {
      onChange(pasted)
      inputRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus()
    }
  }

  return (
    <div>
      <span className="field-label">Enter the 6-digit code</span>
      <div className="flex gap-2" onPaste={handlePaste}>
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(el) => (inputRefs.current[index] = el)}
            value={digit.trim()}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            disabled={disabled}
            inputMode="numeric"
            maxLength={1}
            aria-label={`Digit ${index + 1} of ${OTP_LENGTH}`}
            className={`input-field h-12 w-11 text-center text-lg font-semibold ${
              error ? 'input-field-error' : ''
            }`}
          />
        ))}
      </div>
      {error && <p className="field-error">{error}</p>}
    </div>
  )
}
