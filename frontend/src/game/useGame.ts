import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, createGame, getGame, rollDice, submitTurn } from '../api/client'
import type { From, GameView, Move, NodeView, To } from '../api/types'
import { currentNode, sources, startWalk, step, targets, undo, type TargetOption, type TurnWalk } from './turn'

/** Somewhere on the board the player can click: a point, the bar or the bear-off tray. */
export type Location = number | 'bar' | 'off'

export interface GameController {
  game: GameView | null
  /** The position within the current turn, or null once the game is over. */
  node: NodeView | null
  walk: TurnWalk
  selected: From | null
  movable: From[]
  destinations: TargetOption[]
  busy: boolean
  error: string | null
  pick(location: Location): void
  /** Rolls for the player to act: their opening die, or their dice for the turn. */
  roll(): void
  undo(): void
  confirm(): void
  pass(): void
  playAgain(): void
  retry(): void
}

export function useGame(): GameController {
  const [game, setGame] = useState<GameView | null>(null)
  const [walk, setWalk] = useState<TurnWalk>(startWalk)
  const [selected, setSelected] = useState<From | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const show = useCallback((next: GameView) => {
    setGame(next)
    setWalk(startWalk())
    setSelected(null)
  }, [])

  const begin = useCallback(async () => {
    setBusy(true)
    setError(null)
    try {
      show(await createGame())
    } catch (e) {
      setError(describe(e))
    } finally {
      setBusy(false)
    }
  }, [show])

  // Create exactly one game on load, even though StrictMode runs effects twice in development.
  const started = useRef(false)
  useEffect(() => {
    if (!started.current) {
      started.current = true
      void begin()
    }
  }, [begin])

  const turn = game?.turn ?? null
  const node = turn ? currentNode(turn, walk) : null
  const movable = node ? sources(node) : []
  const destinations = node && selected !== null ? targets(node, selected) : []

  const pick = (location: Location) => {
    if (!turn || !node || busy) return
    if (selected !== null && destinations.some((t) => t.to === location)) {
      setWalk(step(turn, walk, selected, location as To))
      setSelected(null)
    } else if (location !== 'off' && movable.includes(location) && location !== selected) {
      setSelected(location)
    } else {
      setSelected(null)
    }
  }

  /** Sends an action to the server and shows the game it returns. */
  const act = async (request: (current: GameView) => Promise<GameView>) => {
    if (!game || busy) return
    setBusy(true)
    setError(null)
    try {
      show(await request(game))
    } catch (e) {
      setError(describe(e))
      // The server refused the action (out of turn, stale or illegal); reload its view so the board matches.
      if (e instanceof ApiError && (e.status === 409 || e.status === 422)) {
        show(await getGame(game.id).catch(() => game))
      }
    } finally {
      setBusy(false)
    }
  }
  const submit = (moves: Move[]) => act((g) => submitTurn(g.id, g.turnNumber, moves))

  const retry = () => {
    // With no game, or a finished one, the failed action was creating a game, so try that again.
    if (!game || game.status === 'FINISHED') {
      void begin()
    } else {
      setBusy(true)
      setError(null)
      getGame(game.id)
        .then(show)
        .catch((e) => setError(describe(e)))
        .finally(() => setBusy(false))
    }
  }

  return {
    game,
    node,
    walk,
    selected,
    movable,
    destinations,
    busy,
    error,
    pick,
    roll: () => {
      if (game?.phase === 'OPENING' || game?.phase === 'ROLLING') {
        void act((g) => rollDice(g.id, g.turnNumber, g.toMove))
      }
    },
    undo: () => {
      setWalk(undo(walk))
      setSelected(null)
    },
    confirm: () => {
      if (node?.finished) void submit([...walk.moves])
    },
    pass: () => {
      if (turn?.mustPass) void submit([])
    },
    playAgain: () => void begin(),
    retry,
  }
}

function describe(e: unknown): string {
  // A 5xx without problem details comes from the dev proxy when the backend is down.
  if (e instanceof ApiError && (e.problem || e.status < 500)) return e.message
  return 'Could not reach the server. Is the backend running?'
}
