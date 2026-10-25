import './Dice.css'

// Pip positions on a 3x3 grid, numbered 0..8 left to right, top to bottom.
const PIPS: Record<number, number[]> = {
  1: [4],
  2: [2, 6],
  3: [2, 4, 6],
  4: [0, 2, 6, 8],
  5: [0, 2, 4, 6, 8],
  6: [0, 2, 3, 5, 6, 8],
}

export type DieTone = 'ivory' | 'charcoal'

export interface DieProps {
  value: number
  /** Ivory for WHITE (and turn dice), charcoal for BLACK's opening die. */
  tone?: DieTone
  /** Already played this turn, or a past opening round. */
  dimmed?: boolean
  /** Stagger for the throw animation, in milliseconds. */
  delay?: number
}

/** One die. It plays a short throw animation when it first appears. */
export function Die({ value, tone = 'ivory', dimmed = false, delay = 0 }: DieProps) {
  return (
    <div className={`die ${tone}${dimmed ? ' used' : ''}`} style={{ animationDelay: `${delay}ms` }}
      aria-label={`${value}`}>
      {Array.from({ length: 9 }, (_, cell) => (
        <span key={cell} className={PIPS[value].includes(cell) ? 'pip' : undefined} />
      ))}
    </div>
  )
}

/** Where a die will land once thrown. */
export function DieSlot({ tone = 'ivory' }: { tone?: DieTone }) {
  return <div className={`die-slot ${tone}`} aria-hidden="true" />
}

export interface DiceProps {
  /** The roll; doubles are shown as four dice. */
  roll: [number, number]
  /** Dice not yet played this turn. */
  unused: number[]
}

/** The dice for a turn, dimming those already played. */
export function Dice({ roll, unused }: DiceProps) {
  const shown = roll[0] === roll[1] ? [roll[0], roll[0], roll[0], roll[0]] : roll
  const remaining = [...unused]
  const dice = shown.map((value) => {
    const i = remaining.indexOf(value)
    if (i >= 0) remaining.splice(i, 1)
    return { value, used: i < 0 }
  })
  return (
    <div className="dice" aria-label={`Dice: ${shown.join(', ')}`}>
      {dice.map((d, i) => (
        <Die key={i} value={d.value} dimmed={d.used} delay={i * 60} />
      ))}
    </div>
  )
}
