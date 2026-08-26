import { useEffect, useRef, useState } from 'react'

/**
 * Counts down from `seconds` to 0. Call restart() to run it again
 * (e.g. after tapping "Resend code").
 */
export function useCountdown(seconds) {
  const [remaining, setRemaining] = useState(seconds)
  const intervalRef = useRef(null)

  useEffect(() => {
    intervalRef.current = setInterval(() => {
      setRemaining((prev) => (prev <= 0 ? 0 : prev - 1))
    }, 1000)
    return () => clearInterval(intervalRef.current)
  }, [])

  function restart() {
    setRemaining(seconds)
  }

  return { remaining, isFinished: remaining <= 0, restart }
}
