// Walking the server's turn graph one checker at a time. Everything here reads the graph; nothing decides
// legality on its own.

import type { EdgeView, From, Move, NodeView, To, TurnView } from '../api/types'

/** Where the player is within a turn: the node ids visited (starting at node 0) and the moves taken. */
export interface TurnWalk {
  readonly path: readonly number[]
  readonly moves: readonly Move[]
}

export interface TargetOption {
  to: To
  hit: boolean
}

export function startWalk(): TurnWalk {
  return { path: [0], moves: [] }
}

export function currentNode(turn: TurnView, walk: TurnWalk): NodeView {
  return turn.nodes[walk.path[walk.path.length - 1]]
}

/** Checkers that can move from this node: distinct origins, in the server's order. */
export function sources(node: NodeView): From[] {
  return distinct(node.moves.map((m) => m.from))
}

/** Where the checker at `from` can go from this node. */
export function targets(node: NodeView, from: From): TargetOption[] {
  const options: TargetOption[] = []
  for (const edge of node.moves) {
    if (edge.from === from && !options.some((o) => o.to === edge.to)) {
      options.push({ to: edge.to, hit: edge.hit })
    }
  }
  return options
}

/**
 * Moves the checker at `from` to `to`. If more than one die makes that move (only possible when
 * bearing off), the lowest is used so the higher die stays available; the player can undo to choose otherwise.
 */
export function step(turn: TurnView, walk: TurnWalk, from: From, to: To): TurnWalk {
  const matching = currentNode(turn, walk).moves.filter((m) => m.from === from && m.to === to)
  if (matching.length === 0) {
    throw new Error(`No legal move from ${from} to ${to}`)
  }
  const edge = matching.reduce((low: EdgeView, m) => (m.die < low.die ? m : low))
  return {
    path: [...walk.path, edge.next],
    moves: [...walk.moves, { from: edge.from, to: edge.to, die: edge.die }],
  }
}

export function undo(walk: TurnWalk): TurnWalk {
  if (walk.path.length === 1) {
    return walk
  }
  return { path: walk.path.slice(0, -1), moves: walk.moves.slice(0, -1) }
}

export function canConfirm(node: NodeView): boolean {
  return node.finished
}

function distinct<T>(values: T[]): T[] {
  return values.filter((v, i) => values.indexOf(v) === i)
}
