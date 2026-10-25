import type { BoardView, Player } from '../api/types'
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
}

const PLAYERS: Player[] = ['WHITE', 'BLACK']

export function Board({ board, mover }: BoardProps) {
  const slots = pointSlots(mover)
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
                label={i === checkers.length - 1 ? label : null} />
            ))}
          </g>
        )
      })}

      {PLAYERS.map((owner) => {
        const { checkers, label } = barStack(mover, owner, board.bar[owner])
        return checkers.map((c, i) => (
          <Checker key={`${owner}-${i}`} x={c.x} y={c.y} owner={owner}
            label={i === checkers.length - 1 ? label : null} />
        ))
      })}

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
    </svg>
  )
}

function Point({ slot }: { slot: PointSlot }) {
  const half = POINT_WIDTH / 2 - 3
  const apex = slot.baseY + slot.direction * POINT_HEIGHT
  const d = `M ${slot.x - half} ${slot.baseY} L ${slot.x + half} ${slot.baseY} L ${slot.x} ${apex} Z`
  return <path className={slot.dark ? 'point point-dark' : 'point point-light'} d={d} />
}

function Checker({ x, y, owner, label }: { x: number; y: number; owner: Player; label: number | null }) {
  const kind = owner.toLowerCase()
  return (
    <g className={`checker checker-${kind}`}>
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
      <line x1={BAR_X + BAR_WIDTH / 2} y1={y - 22} x2={BAR_X + BAR_WIDTH / 2} y2={y + 22} />
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
      <linearGradient id="brass" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#e9c46a" />
        <stop offset="50%" stopColor="#b8892f" />
        <stop offset="100%" stopColor="#8a6420" />
      </linearGradient>
    </defs>
  )
}
