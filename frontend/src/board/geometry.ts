// Board layout in SVG units. The board is drawn from the side of the player to move, as if they sat at it.
// White's home (pips 1-6) is bottom-right next to the bear-off tray and White moves counter-clockwise. Black
// sees the whole case turned half a turn about its centre, so their home and the tray are bottom-left and they
// move clockwise. The case has a tray at each end, so it turns onto itself and the playing field never moves.
// This is drawing only; legality comes from the server's turn graph.

import type { Player } from '../api/types'

export const FRAME = 24
export const POINT_WIDTH = 64
export const BAR_WIDTH = 64
export const TRAY_WIDTH = 80
const HALF_WIDTH = POINT_WIDTH * 6
export const FIELD_HEIGHT = 640
export const POINT_HEIGHT = 260
export const CHECKER_RADIUS = POINT_WIDTH / 2 - 3
export const MAX_VISIBLE = 5

/** The empty tray at the far end of the case from White's home. */
export const SPARE_TRAY_X = FRAME
export const LEFT_HALF_X = SPARE_TRAY_X + TRAY_WIDTH + FRAME
export const BAR_X = LEFT_HALF_X + HALF_WIDTH
export const RIGHT_HALF_X = BAR_X + BAR_WIDTH
export const TRAY_X = RIGHT_HALF_X + HALF_WIDTH + FRAME
export const WIDTH = TRAY_X + TRAY_WIDTH + FRAME
export const FIELD_TOP = FRAME
export const FIELD_BOTTOM = FRAME + FIELD_HEIGHT
export const HEIGHT = FIELD_BOTTOM + FRAME
export const HALF_WIDTH_UNITS = HALF_WIDTH

/**
 * Left edge, on the mover's side, of a box `width` wide whose left edge is at `x` on White's side. Things in
 * the case are symmetric top to bottom or placed by the mover's side, so a half turn only swaps left and right.
 */
export function turnX(mover: Player, x: number, width = 0): number {
  return mover === 'WHITE' ? x : WIDTH - x - width
}

/** Converts between a player's pip (1 = next to bearing off) and an absolute point. Its own inverse. */
export function pipToPoint(mover: Player, pip: number): number {
  return mover === 'WHITE' ? pip : 25 - pip
}

export interface PointSlot {
  /** Absolute point 1..24. */
  point: number
  /** The mover's number for this point (1..24). */
  pip: number
  /** Horizontal centre of the point's column. */
  x: number
  top: boolean
  /** y of the edge the point's triangle and checker stack start from. */
  baseY: number
  /** Which way stacks grow: +1 down from the top edge, -1 up from the bottom edge. */
  direction: 1 | -1
  /** Alternating stain, inlaid in the wood: it turns with the board. */
  dark: boolean
}

export function pointSlot(mover: Player, point: number): PointSlot {
  // Laid out as White sees it (White's pip is the point), then turned for Black.
  let column: number
  let left: number
  if (point <= 6) {
    left = RIGHT_HALF_X
    column = 6 - point
  } else if (point <= 12) {
    left = LEFT_HALF_X
    column = 12 - point
  } else if (point <= 18) {
    left = LEFT_HALF_X
    column = point - 13
  } else {
    left = RIGHT_HALF_X
    column = point - 19
  }
  const top = (point > 12) === (mover === 'WHITE')
  return {
    point,
    pip: pipToPoint(mover, point),
    x: turnX(mover, left + column * POINT_WIDTH + POINT_WIDTH / 2),
    top,
    baseY: top ? FIELD_TOP : FIELD_BOTTOM,
    direction: top ? 1 : -1,
    dark: point % 2 === 1,
  }
}

/** All 24 slots, ordered by absolute point. */
export function pointSlots(mover: Player): PointSlot[] {
  return Array.from({ length: 24 }, (_, i) => pointSlot(mover, i + 1))
}

export interface Stack {
  /** Centres of the checkers to draw, from the base outwards. */
  checkers: { x: number; y: number }[]
  /** Shown on the outermost checker when there are more than fit. */
  label: number | null
}

/** Lays out `count` checkers from a base edge, capping the visible stack and labelling the rest. */
export function stack(x: number, baseY: number, direction: 1 | -1, count: number): Stack {
  const visible = Math.min(count, MAX_VISIBLE)
  const checkers = Array.from({ length: visible }, (_, i) => ({
    x,
    y: baseY + direction * (CHECKER_RADIUS + 1 + i * (CHECKER_RADIUS * 2 + 1)),
  }))
  return { checkers, label: count > MAX_VISIBLE ? count : null }
}

const CENTER_Y = FIELD_TOP + FIELD_HEIGHT / 2
const BAR_GAP = 16

/** Checkers on the bar: the mover's in the bottom half, the opponent's in the top half, growing outwards. */
export function barStack(mover: Player, owner: Player, count: number): Stack {
  const mine = owner === mover
  return stack(turnX(mover, BAR_X + BAR_WIDTH / 2), mine ? CENTER_Y + BAR_GAP : CENTER_Y - BAR_GAP, mine ? 1 : -1, count)
}

export const SLAB_HEIGHT = 14

export interface TrayStack {
  /** Top-left corners of the borne-off slabs. */
  slabs: { x: number; y: number }[]
  /** Which end of the tray this player's slabs start from. */
  top: boolean
}

/** Borne-off checkers as thin slabs: the mover's from the bottom of the tray, the opponent's from the top. */
export function trayStack(mover: Player, owner: Player, count: number): TrayStack {
  const top = owner !== mover
  const x = turnX(mover, TRAY_X, TRAY_WIDTH) + 8
  const slabs = Array.from({ length: count }, (_, i) => ({
    x,
    y: top ? FIELD_TOP + 8 + i * (SLAB_HEIGHT + 2) : FIELD_BOTTOM - 8 - SLAB_HEIGHT - i * (SLAB_HEIGHT + 2),
  }))
  return { slabs, top }
}
