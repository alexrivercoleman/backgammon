import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, createGame, getGame, rollDice, submitTurn } from '../api/client'
import type { From, GameView, Move, NodeView, To } from '../api/types'
import { linkedGameId, linkTo } from './link'
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
  /** What the error's button says: "New game" once the server has lost the game, otherwise "Retry". */
  retryLabel: string
  pick(location: Location): void
  /** Rolls for the player to act: their opening die, or their dice for the turn. */
  roll(): void
  undo(): void
  confirm(): void
  pass(): void
  playAgain(): void
  retry(): void
}

type Request = (current: GameView) => Promise<GameView>

/** What Retry does after an error. */
type Recovery =
  /** Creating a game failed, or the server no longer has this one: start a new game. */
  | { kind: 'new-game' }
  /** The server refused the action and the board was reloaded: reload it again. */
  | { kind: 'reload' }
  /** The request may not have arrived: repeat it, unless the reloaded game shows it did. */
  | { kind: 'repeat'; request: Request }

interface Failure {
  message: string
  label: string
  recovery: Recovery
}

export function useGame(): GameController {
  const [game, setGame] = useState<GameView | null>(null)
  const [walk, setWalk] = useState<TurnWalk>(startWalk)
  const [selected, setSelected] = useState<From | null>(null)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<Failure | null>(null)

  const show = useCallback((next: GameView) => {
    setGame(next)
    setWalk(startWalk())
    setSelected(null)
    if (linkedGameId(window.location.search) !== next.id) {
      window.history.replaceState(null, '', linkTo(window.location.search, next.id))
    }
  }, [])

  /** Reports an error; `recovery` applies unless the server lost the game or refused the action. */
  const fail = useCallback((e: unknown, recovery: Recovery) => {
    if (e instanceof ApiError && e.status === 404) {
      setFailure({ message: 'This game is no longer on the server.', label: 'New game', recovery: { kind: 'new-game' } })
    } else {
      setFailure({ message: describe(e), label: 'Retry', recovery: refused(e) ? { kind: 'reload' } : recovery })
    }
  }, [])

  /** Reopens game `id` if the server still has it; otherwise, or with no id, starts a new game. */
  const begin = useCallback(async (id: string | null = null) => {
    setBusy(true)
    setFailure(null)
    try {
      show(await (id ? reopen(id) : createGame()))
    } catch (e) {
      setFailure({ message: describe(e), label: 'Retry', recovery: { kind: 'new-game' } })
    } finally {
      setBusy(false)
    }
  }, [show])

  // Open exactly one game on load, even though StrictMode runs effects twice in development.
  const started = useRef(false)
  useEffect(() => {
    if (!started.current) {
      started.current = true
      void begin(linkedGameId(window.location.search))
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

  /** Sends an action for `base` to the server and shows the game it returns. */
  const perform = async (base: GameView, request: Request) => {
    try {
      show(await request(base))
    } catch (e) {
      fail(e, { kind: 'repeat', request })
      // The server refused the action (out of turn, stale or illegal); reload its view so the board matches.
      if (refused(e)) {
        show(await getGame(base.id).catch(() => base))
      }
    }
  }

  const act = async (request: Request) => {
    if (!game || busy) return
    setBusy(true)
    setFailure(null)
    try {
      await perform(game, request)
    } finally {
      setBusy(false)
    }
  }
  const submit = (moves: Move[]) => act((g) => submitTurn(g.id, g.turnNumber, moves))

  const retry = async () => {
    if (!failure || busy) return
    const { recovery } = failure
    if (!game || recovery.kind === 'new-game') {
      // Before any game has loaded, try the linked one again rather than abandoning it.
      await begin(game ? null : linkedGameId(window.location.search))
      return
    }
    setBusy(true)
    setFailure(null)
    try {
      // Reload first: the failed request may have reached the server after all, and must not be applied twice.
      const fresh = await getGame(game.id)
      if (recovery.kind === 'repeat' && sameStep(fresh, game)) {
        await perform(fresh, recovery.request)
      } else {
        show(fresh)
      }
    } catch (e) {
      fail(e, recovery)
    } finally {
      setBusy(false)
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
    error: failure?.message ?? null,
    retryLabel: failure?.label ?? 'Retry',
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
    retry: () => void retry(),
  }
}

/** Loads game `id`, or starts a new game if the server no longer has it. */
async function reopen(id: string): Promise<GameView> {
  try {
    return await getGame(id)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return createGame()
    throw e
  }
}

/** The server refused the action: out of turn, stale or illegal. */
function refused(e: unknown): boolean {
  return e instanceof ApiError && (e.status === 409 || e.status === 422)
}

/** Whether two views of a game are waiting for the same action; any action that went through changes this. */
function sameStep(a: GameView, b: GameView): boolean {
  return a.turnNumber === b.turnNumber && a.phase === b.phase && a.toMove === b.toMove
}

function describe(e: unknown): string {
  // A 5xx without problem details comes from the dev proxy when the backend is down.
  if (e instanceof ApiError && (e.problem || e.status < 500)) return e.message
  return 'Could not reach the server. Is the backend running?'
}
