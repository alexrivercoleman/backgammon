// Board layout in SVG units. The board is drawn from the perspective of the player to move: their home
// (pips 1-6) is bottom-right next to the bear-off tray, and they move counter-clockwise. Switching player
// mirrors the board top to bottom. This is drawing only; legality comes from the server's turn graph.

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

export const LEFT_HALF_X = FRAME
export const BAR_X = LEFT_HALF_X + HALF_WIDTH
export const RIGHT_HALF_X = BAR_X + BAR_WIDTH
export const TRAY_X = RIGHT_HALF_X + HALF_WIDTH + FRAME
export const WIDTH = TRAY_X + TRAY_WIDTH + FRAME
export const FIELD_TOP = FRAME
export const FIELD_BOTTOM = FRAME + FIELD_HEIGHT
export const HEIGHT = FIELD_BOTTOM + FRAME
export const HALF_WIDTH_UNITS = HALF_WIDTH

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
  /** Alternating stain, fixed by screen position so the board pattern stays put when it flips. */
  dark: boolean
}

export function pointSlot(mover: Player, point: number): PointSlot {
  const pip = pipToPoint(mover, point)
  let column: number
  let left: number
  if (pip <= 6) {
    left = RIGHT_HALF_X
    column = 6 - pip
  } else if (pip <= 12) {
    left = LEFT_HALF_X
    column = 12 - pip
  } else if (pip <= 18) {
    left = LEFT_HALF_X
    column = pip - 13
  } else {
    left = RIGHT_HALF_X
    column = pip - 19
  }
  const top = pip > 12
  return {
    point,
    pip,
    x: left + column * POINT_WIDTH + POINT_WIDTH / 2,
    top,
    baseY: top ? FIELD_TOP : FIELD_BOTTOM,
    direction: top ? 1 : -1,
    dark: pip % 2 === 1,
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
  return stack(BAR_X + BAR_WIDTH / 2, mine ? CENTER_Y + BAR_GAP : CENTER_Y - BAR_GAP, mine ? 1 : -1, count)
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
  const x = TRAY_X + 8
  const slabs = Array.from({ length: count }, (_, i) => ({
    x,
    y: top ? FIELD_TOP + 8 + i * (SLAB_HEIGHT + 2) : FIELD_BOTTOM - 8 - SLAB_HEIGHT - i * (SLAB_HEIGHT + 2),
  }))
  return { slabs, top }
}
