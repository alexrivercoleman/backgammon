import type { ReactNode } from 'react'
import type { GameView, NodeView, Player } from '../api/types'
import type { GameController } from '../game/useGame'
import { Dice, Die, DieSlot } from './Dice'
import './TurnPanel.css'

const NAMES: Record<Player, string> = { WHITE: 'White', BLACK: 'Black' }

/** Whose turn it is, the dice, a hint for the next step, and the buttons for what the game is waiting for. */
export function TurnPanel({ controller }: { controller: GameController }) {
  const { game, node, busy } = controller
  if (!game || game.phase === 'FINISHED') return null

  let title: string
  let hint: string
  let dice: ReactNode
  let actions: ReactNode
  if (game.phase === 'MOVING' && game.dice && game.turn && node) {
    title = `${NAMES[game.toMove]} to move`
    hint = moveHint(game.turn.mustPass, node, controller.selected !== null)
    dice = <Dice roll={game.dice} unused={node.unusedDice} />
    actions = (
      <>
        <button type="button" onClick={controller.undo} disabled={busy || controller.walk.moves.length === 0}>
          Undo
        </button>
        {game.turn.mustPass ? (
          <button type="button" className="primary" onClick={controller.pass} disabled={busy}>Pass</button>
        ) : (
          <button type="button" className="primary" onClick={controller.confirm} disabled={busy || !node.finished}>
            Confirm
          </button>
        )}
      </>
    )
  } else if (game.phase === 'OPENING') {
    title = `${NAMES[game.toMove]}, roll for the start`
    hint = openingHint(game)
    dice = <OpeningDice game={game} />
    actions = <RollButton label="Roll die" onRoll={controller.roll} busy={busy} />
  } else {
    title = `${NAMES[game.toMove]} to roll`
    hint = 'Roll the dice to start your turn.'
    dice = (
      <div className="dice">
        <DieSlot />
        <DieSlot />
      </div>
    )
    actions = <RollButton label="Roll dice" onRoll={controller.roll} busy={busy} />
  }

  return (
    <section className="turn-panel">
      <div className="to-move">
        <span className={`swatch swatch-${game.toMove.toLowerCase()}`} aria-hidden="true" />
        <div>
          <strong>{title}</strong>
          <span className="hint">{hint}</span>
        </div>
      </div>
      {dice}
      <div className="actions">{actions}</div>
    </section>
  )
}

/** Focused on appearance, so the player whose turn it is can roll with Enter or Space. */
function RollButton({ label, onRoll, busy }: { label: string; onRoll: () => void; busy: boolean }) {
  return (
    <button type="button" className="primary roll" onClick={onRoll} disabled={busy} autoFocus>
      {label}
    </button>
  )
}

/** White's die and Black's die for the current opening round; after a tie, the tied pair is shown dimmed. */
function OpeningDice({ game }: { game: GameView }) {
  const tie = game.openingDie === null ? game.openingThrows.at(-1) : undefined
  const white = game.openingDie ?? tie?.[0]
  const black = tie?.[1]
  // Keys change with each round so every freshly thrown die replays its throw animation.
  const round = game.openingThrows.length
  return (
    <div className="opening-dice">
      <figure>
        {white !== undefined ? <Die value={white} dimmed={tie !== undefined} key={`w${round}${tie ? '-tie' : ''}`} />
          : <DieSlot />}
        <figcaption>White</figcaption>
      </figure>
      <figure>
        {black !== undefined ? <Die value={black} tone="charcoal" dimmed key={`b${round}`} />
          : <DieSlot tone="charcoal" />}
        <figcaption>Black</figcaption>
      </figure>
    </div>
  )
}

function openingHint(game: GameView): string {
  if (game.openingDie !== null) return `White threw ${game.openingDie}. Higher number starts.`
  const tie = game.openingThrows.at(-1)
  if (tie) return `Tie at ${tie[0]} — both throw again.`
  return 'One die each; the higher number starts.'
}

function moveHint(mustPass: boolean, node: NodeView, selecting: boolean): string {
  if (mustPass) return 'No legal moves — pass the dice.'
  if (node.finished) return 'Turn complete — confirm, or undo to change it.'
  if (selecting) return 'Choose where it goes.'
  return 'Pick a checker to move.'
}
