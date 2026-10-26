import { useLayoutEffect, useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { Board, type BoardProps } from './Board'
import { HEIGHT, WIDTH } from './geometry'

const TURN_AWAY = turn(0, 90)
const TURN_BACK = turn(-90, 0)

/**
 * The board, turned round on the table for the next player: when the mover changes, the old position turns a
 * quarter of the way about the centre and the new one, already half a turn round, completes it. The two meet
 * exactly at the quarter, so the swap can't be seen. Clicks are ignored mid-turn.
 */
export function SpinningBoard(props: BoardProps) {
  const wrapper = useRef<HTMLDivElement>(null)
  const latest = useRef(props)
  const turns = useRef(0)
  const [frozen, setFrozen] = useState<BoardProps | null>(null)

  useLayoutEffect(() => {
    const previous = latest.current
    latest.current = props
    const el = wrapper.current
    if (props.mover === previous.mover || !el || prefersReducedMotion()) return

    // Runs before paint, so the new board is never seen before the turn starts. If a turn is already
    // running, it is abandoned: only the latest turn may unfreeze the board.
    const current = ++turns.current
    el.getAnimations().forEach((a) => a.cancel())
    setFrozen({ ...previous, onPick: undefined, movable: [], destinations: [], selected: null })
    const away = el.animate(TURN_AWAY, { duration: 300, easing: 'ease-in', fill: 'forwards' })
    away.finished.then(
      () => {
        if (current !== turns.current) return
        // The new board must be in the DOM before the second half starts, or the old one shows a frame early.
        flushSync(() => setFrozen(null))
        el.animate(TURN_BACK, { duration: 380, easing: 'cubic-bezier(0.2, 0.8, 0.3, 1)' })
        away.cancel()
      },
      () => {
        if (current === turns.current) setFrozen(null)
      },
    )
  }, [props])

  return (
    <div ref={wrapper} className="turntable">
      <Board {...(frozen ?? props)} />
    </div>
  )
}

/** Keyframes turning from one angle to another, shrunk at each step so the wide board stays within its height. */
function turn(from: number, to: number): Keyframe[] {
  const steps = 10
  return Array.from({ length: steps + 1 }, (_, i) => {
    const angle = from + ((to - from) * i) / steps
    const a = (Math.abs(angle) * Math.PI) / 180
    const scale = Math.min(1, HEIGHT / (WIDTH * Math.sin(a) + HEIGHT * Math.cos(a)))
    return { transform: `rotate(${angle}deg) scale(${scale.toFixed(3)})` }
  })
}

function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}
