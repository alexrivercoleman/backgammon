import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, createGame, getGame, getLog, submitTurn } from '../api/client'
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
  /** Opening throws as [white, black], ties included. */
  openingThrows: [number, number][]
  busy: boolean
  error: string | null
  pick(location: Location): void
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
  const [openingThrows, setOpeningThrows] = useState<[number, number][]>([])
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
    setOpeningThrows([])
    try {
      const created = await createGame()
      show(created)
      const log = await getLog(created.id)
      setOpeningThrows(log.filter((e) => e.kind === 'OPENING_THROW' && e.dice).map((e) => e.dice!))
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

  const submit = async (moves: Move[]) => {
    if (!game) return
    setBusy(true)
    setError(null)
    try {
      show(await submitTurn(game.id, game.turnNumber, moves))
    } catch (e) {
      setError(describe(e))
      // The server rejected the turn (stale or illegal); reload its view so the board matches.
      if (e instanceof ApiError && (e.status === 409 || e.status === 422)) {
        show(await getGame(game.id).catch(() => game))
      }
    } finally {
      setBusy(false)
    }
  }

  const retry = () => {
    if (!game) {
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
    openingThrows,
    busy,
    error,
    pick,
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
  if (e instanceof ApiError) return e.message
  return 'Could not reach the server. Is the backend running?'
}
