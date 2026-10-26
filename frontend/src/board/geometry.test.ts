import { describe, expect, it } from 'vitest'
import type { Player } from '../api/types'
import {
  BAR_WIDTH,
  BAR_X,
  FIELD_BOTTOM,
  FIELD_TOP,
  MAX_VISIBLE,
  SPARE_TRAY_X,
  TRAY_WIDTH,
  TRAY_X,
  WIDTH,
  barStack,
  pipToPoint,
  pointSlot,
  pointSlots,
  stack,
  trayStack,
  turnX,
  type PointSlot,
} from './geometry'

const players: Player[] = ['WHITE', 'BLACK']

describe('board geometry', () => {
  it.each(players)('gives every point its own column and row for %s', (mover) => {
    const slots = pointSlots(mover)
    const keys = new Set(slots.map((s) => `${s.x}:${s.top}`))
    expect(keys.size).toBe(24)
  })

  it.each(players)("puts %s's home bottom row with pip 1 next to their tray", (mover) => {
    const slots = pointSlots(mover)
    const tray = turnX(mover, TRAY_X, TRAY_WIDTH) + TRAY_WIDTH / 2
    const distance = (s: PointSlot) => Math.abs(s.x - tray)
    const one = slots.find((s) => s.pip === 1)!
    const bottom = slots.filter((s) => !s.top)
    expect(one.top).toBe(false)
    expect(distance(one)).toBe(Math.min(...bottom.map(distance)))
    for (let pip = 1; pip <= 6; pip++) {
      const slot = slots.find((s) => s.pip === pip)!
      expect(slot.top).toBe(false)
      expect(distance(slot)).toBeLessThan(WIDTH / 2)
    }
  })

  it("puts White's home bottom-right and Black's bottom-left", () => {
    expect(pointSlot('WHITE', 1).x).toBeGreaterThan(WIDTH / 2)
    expect(pointSlot('BLACK', 24).x).toBeLessThan(WIDTH / 2)
    expect(turnX('WHITE', TRAY_X, TRAY_WIDTH)).toBeGreaterThan(WIDTH / 2)
    expect(turnX('BLACK', TRAY_X, TRAY_WIDTH)).toBe(SPARE_TRAY_X)
  })

  it('turns the board half a turn about its centre when Black is to move', () => {
    for (let point = 1; point <= 24; point++) {
      const white = pointSlot('WHITE', point)
      const black = pointSlot('BLACK', point)
      expect(black.x).toBe(WIDTH - white.x)
      expect(black.top).toBe(!white.top)
    }
    expect(pointSlot('BLACK', 24)).toMatchObject({ pip: 1, top: false })
    expect(pointSlot('BLACK', 1)).toMatchObject({ pip: 24, top: true })
  })

  it('keeps the playing field in place: the case turns onto itself', () => {
    const field = (mover: Player) => pointSlots(mover).map((s) => `${s.x}:${s.top}`).sort()
    expect(field('BLACK')).toEqual(field('WHITE'))
    expect(turnX('BLACK', BAR_X, BAR_WIDTH)).toBe(BAR_X)
    expect(turnX('BLACK', SPARE_TRAY_X, TRAY_WIDTH)).toBe(TRAY_X)
  })

  it('turns the stains with the wood and alternates them along each row', () => {
    const white = pointSlots('WHITE')
    const black = pointSlots('BLACK')
    expect(black.map((s) => s.dark)).toEqual(white.map((s) => s.dark))
    // A half turn lands the pattern on itself, so the board looks the same with the checkers off.
    const byPosition = (slots: PointSlot[]) => slots.map((s) => `${s.x}:${s.top}:${s.dark}`).sort()
    expect(byPosition(black)).toEqual(byPosition(white))
    expect(pointSlot('WHITE', 1).dark).not.toBe(pointSlot('WHITE', 2).dark)
    // Facing points differ, as on a real board.
    expect(pointSlot('WHITE', 1).dark).not.toBe(pointSlot('WHITE', 24).dark)
  })

  it('converts pips to points and back', () => {
    expect(pipToPoint('WHITE', 6)).toBe(6)
    expect(pipToPoint('BLACK', 6)).toBe(19)
    expect(pipToPoint('BLACK', pipToPoint('BLACK', 13))).toBe(13)
  })

  it('stacks checkers away from the base and labels tall stacks', () => {
    const fromTop = stack(100, FIELD_TOP, 1, 3)
    expect(fromTop.checkers).toHaveLength(3)
    expect(fromTop.checkers[1].y).toBeGreaterThan(fromTop.checkers[0].y)
    expect(fromTop.label).toBeNull()

    const tall = stack(100, FIELD_BOTTOM, -1, 8)
    expect(tall.checkers).toHaveLength(MAX_VISIBLE)
    expect(tall.checkers[1].y).toBeLessThan(tall.checkers[0].y)
    expect(tall.label).toBe(8)

    expect(stack(100, FIELD_TOP, 1, 0).checkers).toEqual([])
  })

  it("keeps the mover's bar checkers and borne-off slabs on their side of the board", () => {
    const mine = barStack('BLACK', 'BLACK', 2)
    const theirs = barStack('BLACK', 'WHITE', 1)
    const middle = (FIELD_TOP + FIELD_BOTTOM) / 2
    expect(mine.checkers.every((c) => c.y > middle && c.x > BAR_X)).toBe(true)
    expect(theirs.checkers.every((c) => c.y < middle)).toBe(true)

    expect(trayStack('WHITE', 'WHITE', 3).top).toBe(false)
    expect(trayStack('WHITE', 'BLACK', 3).top).toBe(true)
    expect(trayStack('WHITE', 'WHITE', 15).slabs).toHaveLength(15)
    expect(trayStack('WHITE', 'WHITE', 1).slabs[0].x).toBe(TRAY_X + 8)
    expect(trayStack('BLACK', 'WHITE', 1).slabs[0].x).toBe(SPARE_TRAY_X + 8)
  })
})
