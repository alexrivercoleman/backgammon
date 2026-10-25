// Shapes of the backend's JSON (see README "API"). The client only reads these; it never applies rules.

export type Player = 'WHITE' | 'BLACK'

/** A move's origin: a point number 1..24 or the bar. */
export type From = number | 'bar'
/** A move's destination: a point number 1..24 or borne off. */
export type To = number | 'off'

export interface Move {
  from: From
  to: To
  die: number
}

export interface BoardView {
  /** Signed checker counts for points 1..24 (index 0 is point 1): positive WHITE, negative BLACK. */
  points: number[]
  bar: Record<Player, number>
  off: Record<Player, number>
}

export interface EdgeView extends Move {
  hit: boolean
  /** Id of the node this move leads to. */
  next: number
}

export interface NodeView {
  id: number
  board: BoardView
  unusedDice: number[]
  /** The turn may be submitted once this node is reached. */
  finished: boolean
  moves: EdgeView[]
}

export interface TurnView {
  mustPass: boolean
  /** Node 0 is the position before any move. */
  nodes: NodeView[]
}

export interface GameView {
  id: string
  status: 'IN_PROGRESS' | 'FINISHED'
  turnNumber: number
  toMove: Player
  dice: [number, number] | null
  winner: Player | null
  board: BoardView
  turn: TurnView | null
}

export interface LogEntry {
  seq: number
  kind: 'OPENING_THROW' | 'ROLL' | 'TURN'
  turnNumber: number | null
  player: Player | null
  /** [white, black] for an opening throw, the two dice for a roll. */
  dice: [number, number] | null
  moves: Move[] | null
  createdAt: string
}

/** RFC 9457 problem details returned for errors. */
export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
}
