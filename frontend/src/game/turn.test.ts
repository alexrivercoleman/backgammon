import { describe, expect, it } from 'vitest'
import type { BoardView, EdgeView, NodeView, TurnView } from '../api/types'
import { canConfirm, currentNode, sources, startWalk, step, targets, undo } from './turn'

// Boards are irrelevant to walking the graph, so every node shares one.
const board: BoardView = { points: Array(24).fill(0), bar: { WHITE: 0, BLACK: 0 }, off: { WHITE: 15, BLACK: 15 } }

function node(id: number, unusedDice: number[], moves: EdgeView[], finished = false): NodeView {
  return { id, board, unusedDice, finished, moves }
}

function edge(from: EdgeView['from'], to: EdgeView['to'], die: number, next: number, hit = false): EdgeView {
  return { from, to, die, hit, next }
}

/** A 3-1: 8/5 6/5 in either order reaches node 4; 24/23 hits a blot. */
const threeOne: TurnView = {
  mustPass: false,
  nodes: [
    node(0, [3, 1], [edge(8, 5, 3, 1), edge(6, 5, 1, 2), edge(24, 23, 1, 3, true)]),
    node(1, [1], [edge(6, 5, 1, 4)]),
    node(2, [3], [edge(8, 5, 3, 4)]),
    node(3, [3], [edge(8, 5, 3, 5)]),
    node(4, [], [], true),
    node(5, [], [], true),
  ],
}

/** A 6-4 bearing off checkers on the 4- and 3-points: the 4-point checker can go off with either die. */
const bearOff: TurnView = {
  mustPass: false,
  nodes: [
    node(0, [6, 4], [edge(4, 'off', 6, 2), edge(4, 'off', 4, 1)]),
    node(1, [6], [edge(3, 'off', 6, 3)]),
    node(2, [4], [edge(3, 'off', 4, 3)]),
    node(3, [], [], true),
  ],
}

const pass: TurnView = { mustPass: true, nodes: [node(0, [6, 5], [], true)] }

describe('turn walking', () => {
  it('lists each movable checker once, in server order', () => {
    expect(sources(threeOne.nodes[0])).toEqual([8, 6, 24])
  })

  it('lists destinations for a checker, flagging hits', () => {
    const root = threeOne.nodes[0]
    expect(targets(root, 6)).toEqual([{ to: 5, hit: false }])
    expect(targets(root, 24)).toEqual([{ to: 23, hit: true }])
    expect(targets(root, 13)).toEqual([])
  })

  it('steps through a turn until it can be confirmed', () => {
    let walk = step(threeOne, startWalk(), 8, 5)
    expect(walk.path).toEqual([0, 1])
    expect(walk.moves).toEqual([{ from: 8, to: 5, die: 3 }])
    expect(canConfirm(currentNode(threeOne, walk))).toBe(false)

    walk = step(threeOne, walk, 6, 5)
    expect(currentNode(threeOne, walk).id).toBe(4)
    expect(canConfirm(currentNode(threeOne, walk))).toBe(true)
  })

  it('reaches the same node whichever order the moves are made in', () => {
    const viaEight = step(threeOne, step(threeOne, startWalk(), 8, 5), 6, 5)
    const viaSix = step(threeOne, step(threeOne, startWalk(), 6, 5), 8, 5)

    expect(currentNode(threeOne, viaEight)).toBe(currentNode(threeOne, viaSix))
    expect(viaSix.moves).toEqual([{ from: 6, to: 5, die: 1 }, { from: 8, to: 5, die: 3 }])
  })

  it('undoes one move at a time and stops at the start', () => {
    const two = step(threeOne, step(threeOne, startWalk(), 8, 5), 6, 5)

    const one = undo(two)
    expect(one).toEqual({ path: [0, 1], moves: [{ from: 8, to: 5, die: 3 }] })
    expect(undo(undo(one))).toEqual(startWalk())
  })

  it('does not change the walk it was given', () => {
    const start = startWalk()
    step(threeOne, start, 8, 5)
    expect(start).toEqual(startWalk())
  })

  it('uses the lowest die when more than one makes the move', () => {
    const walk = step(bearOff, startWalk(), 4, 'off')

    expect(walk.moves).toEqual([{ from: 4, to: 'off', die: 4 }])
    expect(currentNode(bearOff, walk).unusedDice).toEqual([6])
  })

  it('rejects a move that is not in the graph', () => {
    expect(() => step(threeOne, startWalk(), 13, 7)).toThrow('No legal move')
  })

  it('lets a blocked player confirm an empty turn straight away', () => {
    const root = currentNode(pass, startWalk())
    expect(sources(root)).toEqual([])
    expect(canConfirm(root)).toBe(true)
  })
})
