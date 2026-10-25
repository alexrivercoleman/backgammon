import { useLayoutEffect, useRef, useState } from 'react'
import { Board, type BoardProps } from './Board'

const TURN_AWAY = [{ transform: 'perspective(1800px) rotateX(0deg)' }, { transform: 'perspective(1800px) rotateX(88deg)' }]
const TURN_BACK = [{ transform: 'perspective(1800px) rotateX(-88deg)' }, { transform: 'perspective(1800px) rotateX(0deg)' }]

/**
 * The board, turned over for the next player: when the mover changes, the old position tips away about the
 * horizontal axis and the new, mirrored one swings back in. Clicks are ignored mid-flip.
 */
export function FlippingBoard(props: BoardProps) {
  const wrapper = useRef<HTMLDivElement>(null)
  const latest = useRef(props)
  const [frozen, setFrozen] = useState<BoardProps | null>(null)

  useLayoutEffect(() => {
    const previous = latest.current
    latest.current = props
    const el = wrapper.current
    if (props.mover === previous.mover || !el || prefersReducedMotion()) return

    // Runs before paint, so the new board is never seen before it has turned away.
    setFrozen({ ...previous, onPick: undefined, movable: [], destinations: [], selected: null })
    const away = el.animate(TURN_AWAY, { duration: 210, easing: 'ease-in', fill: 'forwards' })
    away.finished.then(
      () => {
        setFrozen(null)
        el.animate(TURN_BACK, { duration: 280, easing: 'cubic-bezier(0.2, 0.8, 0.3, 1)' })
        away.cancel()
      },
      () => setFrozen(null),
    )
  }, [props])

  return (
    <div ref={wrapper} className="flipper">
      <Board {...(frozen ?? props)} />
    </div>
  )
}

function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}
