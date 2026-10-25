import { FlippingBoard } from './board/FlippingBoard'
import { GameOver } from './components/GameOver'
import { OpeningBanner } from './components/OpeningBanner'
import { TurnPanel } from './components/TurnPanel'
import { useGame } from './game/useGame'
import './App.css'

function App() {
  const controller = useGame()
  const { game, node, error } = controller

  return (
    <main>
      <header className="title">
        <h1>Τάβλι</h1>
        <span>Portes</span>
      </header>

      {error && (
        <div className="error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={controller.retry}>Retry</button>
        </div>
      )}

      {game ? (
        <>
          <TurnPanel controller={controller} />
          {game.turnNumber === 1 && <OpeningBanner throws={controller.openingThrows} starter={game.toMove} />}
          <div className="table">
            <FlippingBoard
              board={node?.board ?? game.board}
              mover={game.winner ?? game.toMove}
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
