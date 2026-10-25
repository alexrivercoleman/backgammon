import { useState } from 'react'
import type { Player } from '../api/types'
import './GameOver.css'

const NAMES: Record<Player, string> = { WHITE: 'White', BLACK: 'Black' }
const COLOURS = ['#fffaf0', '#2b2b2b', '#1f4e8c', '#e0b84f']

interface Piece {
  left: number
  delay: number
  duration: number
  colour: string
  spin: number
  round: boolean
}

function makePieces(): Piece[] {
  return Array.from({ length: 46 }, (_, i) => ({
    left: Math.random() * 100,
    delay: Math.random() * 0.9,
    duration: 2.2 + Math.random() * 1.6,
    colour: COLOURS[i % COLOURS.length],
    spin: (Math.random() < 0.5 ? -1 : 1) * (360 + Math.random() * 540),
    round: i % 3 === 0,
  }))
}

/** Shown over the board once someone has borne off all fifteen checkers. */
export function GameOver({ winner, turns, onPlayAgain, busy }: {
  winner: Player
  turns: number
  onPlayAgain: () => void
  busy: boolean
}) {
  const [pieces] = useState(makePieces)
  return (
    <div className="game-over">
      <div className="confetti" aria-hidden="true">
        {pieces.map((p, i) => (
          <span key={i} className={p.round ? 'round' : undefined} style={{
            left: `${p.left}%`,
            background: p.colour,
            animationDelay: `${p.delay}s`,
            animationDuration: `${p.duration}s`,
            ['--spin' as string]: `${p.spin}deg`,
          }} />
        ))}
      </div>
      <div className="victory" role="dialog" aria-labelledby="victory-title">
        <span className="kicker">Opa!</span>
        <h2 id="victory-title">
          <span className={`swatch swatch-${winner.toLowerCase()}`} aria-hidden="true" />
          {NAMES[winner]} wins
        </h2>
        <p>All fifteen checkers borne off in {turns} turns.</p>
        <button type="button" className="primary" onClick={onPlayAgain} disabled={busy} autoFocus>
          Play again?
        </button>
      </div>
    </div>
  )
}
