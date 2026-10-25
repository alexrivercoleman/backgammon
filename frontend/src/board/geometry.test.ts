import { describe, expect, it } from 'vitest'
import type { Player } from '../api/types'
import {
  BAR_X,
  FIELD_BOTTOM,
  FIELD_TOP,
  MAX_VISIBLE,
  RIGHT_HALF_X,
  barStack,
  pipToPoint,
  pointSlot,
  pointSlots,
  stack,
  trayStack,
} from './geometry'

const players: Player[] = ['WHITE', 'BLACK']

describe('board geometry', () => {
  it.each(players)('gives every point its own column and row for %s', (mover) => {
    const slots = pointSlots(mover)
    const keys = new Set(slots.map((s) => `${s.x}:${s.top}`))
    expect(keys.size).toBe(24)
  })

  it.each(players)("puts %s's home bottom-right with pip 1 next to the tray", (mover) => {
    const slots = pointSlots(mover)
    const one = slots.find((s) => s.pip === 1)!
    const bottom = slots.filter((s) => !s.top)
    expect(one.top).toBe(false)
    expect(one.x).toBe(Math.max(...bottom.map((s) => s.x)))
    for (let pip = 1; pip <= 6; pip++) {
      const slot = slots.find((s) => s.pip === pip)!
      expect(slot.top).toBe(false)
      expect(slot.x).toBeGreaterThan(RIGHT_HALF_X)
    }
  })

  it('mirrors the board top to bottom when Black is to move', () => {
    for (let point = 1; point <= 24; point++) {
      const white = pointSlot('WHITE', point)
      const black = pointSlot('BLACK', point)
      expect(black.x).toBe(white.x)
      expect(black.top).toBe(!white.top)
    }
    expect(pointSlot('BLACK', 24)).toMatchObject({ pip: 1, top: false })
    expect(pointSlot('BLACK', 1)).toMatchObject({ pip: 24, top: true })
  })

  it('keeps the stain pattern fixed on screen and alternates it along each row', () => {
    const white = pointSlots('WHITE')
    const black = pointSlots('BLACK')
    const byPosition = (slots: typeof white) => slots.map((s) => `${s.x}:${s.top}:${s.dark}`).sort()
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
  })
})
