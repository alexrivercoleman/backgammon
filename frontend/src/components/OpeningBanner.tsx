import type { Player } from '../api/types'

const NAMES: Record<Player, string> = { WHITE: 'White', BLACK: 'Black' }

/** Explains who starts: each opening throw (ties re-rolled), shown during the first turn only. */
export function OpeningBanner({ throws, starter }: { throws: [number, number][]; starter: Player }) {
  if (throws.length === 0) return null
  return (
    <p className="opening">
      <span className="label">Opening throw</span>
      {throws.map(([white, black], i) => (
        <span key={i} className="throw">
          White {white} · Black {black}
          {white === black && <em> — tie, throw again</em>}
        </span>
      ))}
      <strong>{NAMES[starter]} starts.</strong>
    </p>
  )
}
