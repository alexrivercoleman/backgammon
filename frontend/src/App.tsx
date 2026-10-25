import { useEffect, useState } from 'react'
import { createGame } from './api/client'
import type { GameView } from './api/types'
import { Board } from './board/Board'

function App() {
  const [game, setGame] = useState<GameView | null>(null)

  useEffect(() => {
    createGame().then(setGame)
  }, [])

  if (!game) return <p>Setting up the board…</p>
  return <Board board={game.board} mover={game.toMove} />
}

export default App
