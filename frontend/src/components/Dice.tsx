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

export interface DiceProps {
  /** The roll; doubles are shown as four dice. */
  roll: [number, number]
  /** Dice not yet played this turn. */
  unused: number[]
  /** Changes on every new roll, replaying the throw animation. */
  rollKey: string | number
}

export function Dice({ roll, unused, rollKey }: DiceProps) {
  const shown = roll[0] === roll[1] ? [roll[0], roll[0], roll[0], roll[0]] : roll
  const remaining = [...unused]
  const dice = shown.map((value) => {
    const i = remaining.indexOf(value)
    if (i >= 0) remaining.splice(i, 1)
    return { value, used: i < 0 }
  })
  return (
    <div className="dice" key={rollKey} aria-label={`Dice: ${shown.join(', ')}`}>
      {dice.map((d, i) => (
        <div key={i} className={d.used ? 'die used' : 'die'} style={{ animationDelay: `${i * 60}ms` }}>
          {Array.from({ length: 9 }, (_, cell) => (
            <span key={cell} className={PIPS[d.value].includes(cell) ? 'pip' : undefined} />
          ))}
        </div>
      ))}
    </div>
  )
}
