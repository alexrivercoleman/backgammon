import { FlippingBoard } from './board/FlippingBoard'
import { GameOver } from './components/GameOver'
import { HelpButton } from './components/HelpButton'
import { OpeningBanner } from './components/OpeningBanner'
import { TurnPanel } from './components/TurnPanel'
import { useGame } from './game/useGame'
import './App.css'

function App() {
  const controller = useGame()
  const { game, node, error } = controller

  return (
    <main>
      <HelpButton />
      <header className="title">
        <h1>Τάβλι</h1>
        <span>Portes</span>
      </header>

      {error && (
        <div className="error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={controller.retry}>{controller.retryLabel}</button>
        </div>
      )}

      {game ? (
        <>
          <TurnPanel controller={controller} />
          {game.phase === 'MOVING' && game.turnNumber === 1 && (
            <OpeningBanner throws={game.openingThrows} starter={game.toMove} />
          )}
          <div className="table">
            <FlippingBoard
              board={node?.board ?? game.board}
              // During the opening the board stays on White's side; it flips if Black wins the throw.
              mover={game.phase === 'OPENING' ? 'WHITE' : (game.winner ?? game.toMove)}
              movable={controller.movable}
              selected={controller.selected}
              destinations={controller.destinations}
              onPick={node ? controller.pick : undefined}
            />
            {game.winner && (
              <GameOver key={game.id} winner={game.winner} turns={game.turnNumber - 1}
                onPlayAgain={controller.playAgain} busy={controller.busy} />
            )}
          </div>
        </>
      ) : (
        !error && <p className="loading">Setting up the board…</p>
      )}
    </main>
  )
}

export default App
