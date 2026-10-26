import { useEffect, useRef, useState, type MouseEvent, type ReactNode } from 'react'
import {
  BarArt,
  BearOffArt,
  ControlsArt,
  DiceRuleArt,
  DoublesArt,
  GoalArt,
  LandingArt,
  MoveArt,
  OpeningArt,
} from './RuleDiagrams'
import './HelpButton.css'

/** A round "?" in the corner that opens the rules of Portes, written for someone new to backgammon. */
export function HelpButton() {
  const [open, setOpen] = useState(false)
  const dialog = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const el = dialog.current
    if (!el) return
    if (open && !el.open) el.showModal()
    if (!open && el.open) el.close()
  }, [open])

  // Clicks on the backdrop land on the dialog element itself; clicks on the card land inside it.
  const closeOnBackdrop = (e: MouseEvent<HTMLDialogElement>) => {
    if (e.target === e.currentTarget) setOpen(false)
  }

  return (
    <>
      <button type="button" className="help-button" onClick={() => setOpen(true)} aria-label="How to play"
        title="How to play">
        ?
      </button>
      <dialog ref={dialog} className="rules" aria-labelledby="rules-title" onClose={() => setOpen(false)}
        onClick={closeOnBackdrop}>
        <div className="rules-card">
          <header>
            <h2 id="rules-title">How to play <span>Portes</span></h2>
            <button type="button" className="rules-close" onClick={() => setOpen(false)} aria-label="Close">×</button>
          </header>
          <div className="rules-grid">
            <Rule title="The goal" art={<GoalArt />}>
              Race your 15 checkers round the board into <b>your home</b>, then off into the tray. First to
              clear all 15 wins.
            </Rule>
            <Rule title="Who starts" art={<OpeningArt />}>
              Each player rolls one die. Higher starts, playing both dice. A tie? Roll again.
            </Rule>
            <Rule title="Moving" art={<MoveArt />}>
              Move a checker the number on each die. Use one checker for both, or two checkers.
            </Rule>
            <Rule title="Doubles" art={<DoublesArt />}>
              Same number on both dice? Play it four times.
            </Rule>
            <Rule title="Where you can land" art={<LandingArt />}>
              Land anywhere except on <b>two or more</b> of your opponent's checkers. Landing on a single one hits it.
            </Rule>
            <Rule title="Hit? Come back in" art={<BarArt />}>
              A hit checker goes to the bar. Its owner must bring it back in, in the opponent's home, before
              moving anything else.
            </Rule>
            <Rule title="Bearing off" art={<BearOffArt />}>
              Once all 15 are home, move them off. A die bigger than you need takes off your farthest checker.
            </Rule>
            <Rule title="Using your dice" art={<DiceRuleArt />}>
              Play both dice if you can. If only one fits, play the higher. If nothing moves, you pass.
            </Rule>
            <Rule title="On this screen" art={<ControlsArt />}>
              The board turns round on the table for whoever's turn it is, so your home is always at the
              bottom, beside the tray. <b>Undo</b> takes back a move until you confirm.
            </Rule>
          </div>
        </div>
      </dialog>
    </>
  )
}

function Rule({ title, art, children }: { title: string; art: ReactNode; children: ReactNode }) {
  return (
    <section className="rule">
      <div className="rule-art-box">{art}</div>
      <div>
        <h3>{title}</h3>
        <p>{children}</p>
      </div>
    </section>
  )
}
