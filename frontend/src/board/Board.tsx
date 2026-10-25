import type { KeyboardEvent } from 'react'
import type { BoardView, From, Player } from '../api/types'
import type { TargetOption } from '../game/turn'
import type { Location } from '../game/useGame'
import {
  BAR_WIDTH,
  BAR_X,
  CHECKER_RADIUS,
  FIELD_BOTTOM,
  FIELD_HEIGHT,
  FIELD_TOP,
  FRAME,
  HALF_WIDTH_UNITS,
  HEIGHT,
  LEFT_HALF_X,
  MAX_VISIBLE,
  POINT_HEIGHT,
  POINT_WIDTH,
  RIGHT_HALF_X,
  SLAB_HEIGHT,
  TRAY_WIDTH,
  TRAY_X,
  WIDTH,
  barStack,
  pointSlots,
  stack,
  trayStack,
  type PointSlot,
} from './geometry'
import './Board.css'

export interface BoardProps {
  board: BoardView
  /** Whose perspective to draw from: their home is bottom-right. */
  mover: Player
  /** Origins with a legal move; their top checker is highlighted. */
  movable?: From[]
  selected?: From | null
  /** Where the selected checker may go. */
  destinations?: TargetOption[]
  onPick?: (location: Location) => void
}

const PLAYERS: Player[] = ['WHITE', 'BLACK']

export function Board({ board, mover, movable = [], selected = null, destinations = [], onPick }: BoardProps) {
  const slots = pointSlots(mover)
  const sign = mover === 'WHITE' ? 1 : -1
  const stateOf = (from: From) => (selected === from ? 'selected' : movable.includes(from) ? 'movable' : undefined)
  const destination = (to: number | 'off') => destinations.find((d) => d.to === to)
  return (
    <svg className="board" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label="Tavli board">
      <Defs />

      {/* Case: walnut frame, two light playing fields, a hinged bar and the bear-off tray. */}
      <rect className="frame" x={0} y={0} width={WIDTH} height={HEIGHT} rx={18} />
      <rect className="grain" x={0} y={0} width={WIDTH} height={HEIGHT} rx={18} filter="url(#grain-frame)" />
      {[LEFT_HALF_X, RIGHT_HALF_X].map((x) => (
        <g key={x}>
          <rect className="field" x={x} y={FIELD_TOP} width={HALF_WIDTH_UNITS} height={FIELD_HEIGHT} />
          <rect className="grain" x={x} y={FIELD_TOP} width={HALF_WIDTH_UNITS} height={FIELD_HEIGHT}
            filter="url(#grain-field)" />
          <rect className="field-edge" x={x} y={FIELD_TOP} width={HALF_WIDTH_UNITS} height={FIELD_HEIGHT} />
        </g>
      ))}
      <rect className="bar" x={BAR_X} y={0} width={BAR_WIDTH} height={HEIGHT} />
      <rect className="grain" x={BAR_X} y={0} width={BAR_WIDTH} height={HEIGHT} filter="url(#grain-frame)" />
      {/* The fold: where the two halves of the case meet. */}
      <line className="fold" x1={BAR_X + BAR_WIDTH / 2} y1={0} x2={BAR_X + BAR_WIDTH / 2} y2={HEIGHT} />
      <line className="fold-light" x1={BAR_X + BAR_WIDTH / 2 + 1.5} y1={0} x2={BAR_X + BAR_WIDTH / 2 + 1.5}
        y2={HEIGHT} />
      <Hinge y={FIELD_TOP + 70} />
      <Hinge y={FIELD_BOTTOM - 70} />
      <rect className="tray" x={TRAY_X} y={FIELD_TOP} width={TRAY_WIDTH} height={FIELD_HEIGHT} rx={6} />

      {slots.map((slot) => (
        <Point key={slot.point} slot={slot} />
      ))}

      {slots.map((slot) => {
        const value = board.points[slot.point - 1]
        if (value === 0) return null
        const owner: Player = value > 0 ? 'WHITE' : 'BLACK'
        const { checkers, label } = stack(slot.x, slot.baseY, slot.direction, Math.abs(value))
        return (
          <g key={slot.point}>
            {checkers.map((c, i) => (
              <Checker key={i} x={c.x} y={c.y} owner={owner}
                label={i === checkers.length - 1 ? label : null}
                state={i === checkers.length - 1 ? stateOf(slot.point) : undefined} />
            ))}
          </g>
        )
      })}

      {PLAYERS.map((owner) => {
        const { checkers, label } = barStack(mover, owner, board.bar[owner])
        return checkers.map((c, i) => (
          <Checker key={`${owner}-${i}`} x={c.x} y={c.y} owner={owner}
            label={i === checkers.length - 1 ? label : null}
            state={owner === mover && i === checkers.length - 1 ? stateOf('bar') : undefined} />
        ))
      })}

      {/* Ghost checkers where the selected checker can land; red when the move hits a blot. */}
      {slots.map((slot) => {
        const target = destination(slot.point)
        if (!target) return null
        const mine = Math.max(0, sign * board.points[slot.point - 1])
        const index = Math.min(mine, MAX_VISIBLE - 1)
        const at = stack(slot.x, slot.baseY, slot.direction, index + 1).checkers[index]
        return (
          <circle key={slot.point} className={target.hit ? 'ghost ghost-hit' : 'ghost'}
            cx={at.x} cy={at.y} r={CHECKER_RADIUS - 2} />
        )
      })}
      {destination('off') && (
        <rect className="tray-target" x={TRAY_X + 3} y={FIELD_TOP + FIELD_HEIGHT / 2} width={TRAY_WIDTH - 6}
          height={FIELD_HEIGHT / 2 - 3} rx={6} />
      )}

      {PLAYERS.map((owner) =>
        trayStack(mover, owner, board.off[owner]).slabs.map((s, i) => (
          <rect key={`${owner}-${i}`} className={`slab slab-${owner.toLowerCase()}`}
            x={s.x} y={s.y} width={TRAY_WIDTH - 16} height={SLAB_HEIGHT} rx={4} />
        )),
      )}

      {slots.map((slot) => (
        <text key={slot.point} className="pip-label" x={slot.x}
          y={slot.top ? FRAME / 2 : HEIGHT - FRAME / 2} dominantBaseline="central" textAnchor="middle">
          {slot.pip}
        </text>
      ))}

      {onPick && (
        <g className="hit-areas">
          {slots.map((slot) => {
            const value = board.points[slot.point - 1]
            const owner = value > 0 ? 'white' : 'black'
            const contents = value === 0 ? 'empty' : `${Math.abs(value)} ${owner}`
            return (
              <HitArea key={slot.point} location={slot.point} onPick={onPick}
                source={movable.includes(slot.point)} target={destination(slot.point) !== undefined}
                label={`Point ${slot.pip}, ${contents}`}
                x={slot.x - POINT_WIDTH / 2} y={slot.top ? FIELD_TOP : FIELD_TOP + FIELD_HEIGHT / 2}
                width={POINT_WIDTH} height={FIELD_HEIGHT / 2} />
            )
          })}
          <HitArea location="bar" onPick={onPick} source={movable.includes('bar')} target={false}
            label={`Bar, ${board.bar[mover]} of yours`}
            x={BAR_X} y={FIELD_TOP + FIELD_HEIGHT / 2} width={BAR_WIDTH} height={FIELD_HEIGHT / 2} />
          <HitArea location="off" onPick={onPick} source={false} target={destination('off') !== undefined}
            label={`Bear off, ${board.off[mover]} borne off`}
            x={TRAY_X} y={FIELD_TOP} width={TRAY_WIDTH} height={FIELD_HEIGHT} />
        </g>
      )}
    </svg>
  )
}

function Point({ slot }: { slot: PointSlot }) {
  const half = POINT_WIDTH / 2 - 3
  const apex = slot.baseY + slot.direction * POINT_HEIGHT
  const d = `M ${slot.x - half} ${slot.baseY} L ${slot.x + half} ${slot.baseY} L ${slot.x} ${apex} Z`
  return <path className={slot.dark ? 'point point-dark' : 'point point-light'} d={d} />
}

interface HitAreaProps {
  location: Location
  /** Has a checker that can move. */
  source: boolean
  /** Where the selected checker can go. */
  target: boolean
  label: string
  x: number
  y: number
  width: number
  height: number
  onPick: (location: Location) => void
}

function HitArea({ location, source, target, label, onPick, ...rect }: HitAreaProps) {
  const active = source || target
  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      onPick(location)
    }
  }
  return (
    <rect className={active ? 'hit-area active' : 'hit-area'} {...rect} role="button" aria-label={label}
      data-source={source || undefined} data-target={target || undefined}
      tabIndex={active ? 0 : -1} onClick={() => onPick(location)} onKeyDown={onKeyDown} />
  )
}

interface CheckerProps {
  x: number
  y: number
  owner: Player
  label: number | null
  state?: 'movable' | 'selected'
}

function Checker({ x, y, owner, label, state }: CheckerProps) {
  const kind = owner.toLowerCase()
  return (
    <g className={`checker checker-${kind}${state ? ` ${state}` : ''}`}>
      {state && <circle className="checker-ring" cx={x} cy={y} r={CHECKER_RADIUS + 4} filter="url(#aura)" />}
      <circle cx={x} cy={y} r={CHECKER_RADIUS} fill={`url(#checker-${kind})`} filter="url(#shadow)" />
      <circle className="checker-groove" cx={x} cy={y} r={CHECKER_RADIUS * 0.62} />
      {label !== null && (
        <text className="checker-label" x={x} y={y} dominantBaseline="central" textAnchor="middle">
          {label}
        </text>
      )}
    </g>
  )
}

function Hinge({ y }: { y: number }) {
  return (
    <g className="hinge">
      <rect x={BAR_X + 14} y={y - 22} width={BAR_WIDTH - 28} height={44} rx={5} fill="url(#brass)" />
      {/* The barrel the two leaves turn on, sitting on the fold. */}
      <rect className="hinge-barrel" x={BAR_X + BAR_WIDTH / 2 - 3.5} y={y - 25} width={7} height={50} rx={3.5} />
      {[-12, 12].map((dy) => (
        <circle key={`l${dy}`} cx={BAR_X + 22} cy={y + dy} r={2.2} />
      ))}
      {[-12, 12].map((dy) => (
        <circle key={`r${dy}`} cx={BAR_X + BAR_WIDTH - 22} cy={y + dy} r={2.2} />
      ))}
    </g>
  )
}

function Defs() {
  return (
    <defs>
      {/* Wood grain: horizontally stretched noise laid over the wood colour at low opacity. */}
      <filter id="grain-field" x="0" y="0" width="100%" height="100%">
        <feTurbulence type="fractalNoise" baseFrequency="0.004 0.09" numOctaves="3" seed="3" />
        <feColorMatrix type="matrix" values="0 0 0 0 0.35  0 0 0 0 0.2  0 0 0 0 0.08  0 0 0 0.55 0" />
      </filter>
      <filter id="grain-frame" x="0" y="0" width="100%" height="100%">
        <feTurbulence type="fractalNoise" baseFrequency="0.006 0.05" numOctaves="2" seed="11" />
        <feColorMatrix type="matrix" values="0 0 0 0 0.1  0 0 0 0 0.05  0 0 0 0 0.02  0 0 0 0.7 0" />
      </filter>
      <filter id="aura" x="-50%" y="-50%" width="200%" height="200%">
        <feGaussianBlur in="SourceGraphic" stdDeviation="3.5" result="blur" />
        <feMerge>
          <feMergeNode in="blur" />
          <feMergeNode in="blur" />
          <feMergeNode in="SourceGraphic" />
        </feMerge>
      </filter>
      <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
        <feDropShadow dx="1.5" dy="2.5" stdDeviation="2" floodColor="#1a0e05" floodOpacity="0.45" />
      </filter>
      <radialGradient id="checker-white" cx="40%" cy="35%" r="70%">
        <stop offset="0%" stopColor="#fffdf6" />
        <stop offset="70%" stopColor="#efe6d0" />
        <stop offset="100%" stopColor="#d6c8a6" />
      </radialGradient>
      <radialGradient id="checker-black" cx="40%" cy="35%" r="70%">
        <stop offset="0%" stopColor="#5a5a5a" />
        <stop offset="65%" stopColor="#2f2f2f" />
        <stop offset="100%" stopColor="#1b1b1b" />
      </radialGradient>
      <linearGradient id="barrel" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0%" stopColor="#7a5a1c" />
        <stop offset="45%" stopColor="#f0d27e" />
        <stop offset="100%" stopColor="#6b4c17" />
      </linearGradient>
      <linearGradient id="brass" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#e9c46a" />
        <stop offset="50%" stopColor="#b8892f" />
        <stop offset="100%" stopColor="#8a6420" />
      </linearGradient>
    </defs>
  )
}
