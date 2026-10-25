import type { Player } from '../api/types'
import type { GameController } from '../game/useGame'
import { Dice } from './Dice'
import './TurnPanel.css'

const NAMES: Record<Player, string> = { WHITE: 'White', BLACK: 'Black' }

/** Whose turn it is, the dice, a hint for the next step, and the turn's buttons. */
export function TurnPanel({ controller }: { controller: GameController }) {
  const { game, node, walk, selected, busy } = controller
  if (!game || !game.dice || !game.turn || !node) return null
  const mustPass = game.turn.mustPass

  return (
    <section className="turn-panel">
      <div className="to-move">
        <span className={`swatch swatch-${game.toMove.toLowerCase()}`} aria-hidden="true" />
        <div>
          <strong>{NAMES[game.toMove]} to move</strong>
          <span className="hint">{hint(mustPass, node.finished, selected !== null)}</span>
        </div>
      </div>

      <Dice roll={game.dice} unused={node.unusedDice} rollKey={game.turnNumber} />

      <div className="actions">
        <button type="button" onClick={controller.undo} disabled={busy || walk.moves.length === 0}>
          Undo
        </button>
        {mustPass ? (
          <button type="button" className="primary" onClick={controller.pass} disabled={busy}>
            Pass
          </button>
        ) : (
          <button type="button" className="primary" onClick={controller.confirm} disabled={busy || !node.finished}>
            Confirm
          </button>
        )}
      </div>
    </section>
  )
}

function hint(mustPass: boolean, finished: boolean, selecting: boolean): string {
  if (mustPass) return 'No legal moves — pass the dice.'
  if (finished) return 'Turn complete — confirm, or undo to change it.'
  if (selecting) return 'Choose where it goes.'
  return 'Pick a checker to move.'
}
