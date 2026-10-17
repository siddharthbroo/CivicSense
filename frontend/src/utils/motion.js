import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

// Safely register ScrollTrigger in browser environment
if (typeof window !== 'undefined') {
  gsap.registerPlugin(ScrollTrigger)
}

/**
 * Check if user has requested reduced motion in their OS or browser settings
 */
export function prefersReducedMotion() {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export { gsap, ScrollTrigger }

