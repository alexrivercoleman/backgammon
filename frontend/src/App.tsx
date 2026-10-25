import { Board } from './board/Board'
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
          <Board
            board={node?.board ?? game.board}
            mover={game.winner ?? game.toMove}
            movable={controller.movable}
            selected={controller.selected}
            destinations={controller.destinations}
            onPick={node ? controller.pick : undefined}
          />
        </>
      ) : (
        !error && <p className="loading">Setting up the board…</p>
      )}
    </main>
  )
}

export default App
