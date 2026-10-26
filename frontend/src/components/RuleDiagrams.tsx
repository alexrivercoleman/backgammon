// Small illustrations for the rules card, drawn in the board's own wood, stains and checkers. The mini board
// is the real one seen from White's side: home bottom-right, next to the tray, with the spare tray at the far end.

import { createContext, useContext, useId, type ReactNode } from 'react'

const WOOD = { frame: '#5b3a21', field: '#e2c494', light: '#c18c55', dark: '#6b3a1f', tray: '#3a2414' }
const BLUE = '#3d8fe0'

const PIPS: Record<number, [number, number][]> = {
  1: [[1, 1]],
  2: [[0, 2], [2, 0]],
  3: [[0, 2], [1, 1], [2, 0]],
  4: [[0, 0], [0, 2], [2, 0], [2, 2]],
  5: [[0, 0], [0, 2], [1, 1], [2, 0], [2, 2]],
  6: [[0, 0], [0, 2], [1, 0], [1, 2], [2, 0], [2, 2]],
}

function Die({ x, y, value, size = 22, tone = 'ivory', dim = false }: {
  x: number; y: number; value: number; size?: number; tone?: 'ivory' | 'charcoal'; dim?: boolean
}) {
  const step = size / 4
  return (
    <g opacity={dim ? 0.35 : 1}>
      <rect x={x} y={y} width={size} height={size} rx={size * 0.2}
        fill={tone === 'ivory' ? '#fbf6e8' : '#2b2b2b'} stroke={tone === 'ivory' ? '#c8b88f' : '#8a8a8a'} />
      {PIPS[value].map(([col, row], i) => (
        <circle key={i} cx={x + step * (col + 1)} cy={y + step * (row + 1)} r={size * 0.08}
          fill={tone === 'ivory' ? '#2b2118' : '#f3ead6'} />
      ))}
    </g>
  )
}

function Checker({ x, y, tone, r = 6.5, glow = false }: {
  x: number; y: number; tone: 'ivory' | 'charcoal'; r?: number; glow?: boolean
}) {
  return (
    <g>
      {glow && <circle cx={x} cy={y} r={r + 3} fill="none" stroke={BLUE} strokeWidth={2.5} opacity={0.85} />}
      <circle cx={x} cy={y} r={r} fill={tone === 'ivory' ? '#f6eedb' : '#2f2f2f'}
        stroke={tone === 'ivory' ? '#b9a679' : '#111'} strokeWidth={1} />
    </g>
  )
}

/** The id of the enclosing diagram's arrowhead marker; each diagram defines its own. */
const ArrowMarker = createContext('')

function Arrow({ d, dashed = false }: { d: string; dashed?: boolean }) {
  const marker = useContext(ArrowMarker)
  return (
    <path d={d} fill="none" stroke={BLUE} strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round"
      strokeDasharray={dashed ? '5 4' : undefined} markerEnd={`url(#${marker})`} />
  )
}

function Tick({ x, y, ok }: { x: number; y: number; ok: boolean }) {
  return ok ? (
    <path d={`M ${x - 6} ${y} l 4 4 l 8 -9`} fill="none" stroke="#2e8b57" strokeWidth={3} strokeLinecap="round"
      strokeLinejoin="round" />
  ) : (
    <path d={`M ${x - 5} ${y - 5} l 10 10 M ${x + 5} ${y - 5} l -10 10`} stroke="#c0392b" strokeWidth={3}
      strokeLinecap="round" />
  )
}

function Svg({ width, height, label, children }: { width: number; height: number; label: string; children: ReactNode }) {
  const marker = `rule-arrow-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  return (
    <svg className="rule-art" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={label}>
      <defs>
        <marker id={marker} viewBox="0 0 10 10" refX="7" refY="5" markerWidth="5" markerHeight="5"
          orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill={BLUE} />
        </marker>
      </defs>
      <ArrowMarker.Provider value={marker}>{children}</ArrowMarker.Provider>
    </svg>
  )
}

// Mini board: 24 points of 14 units, a bar of 12 and a tray of 20 at each end, drawn 14 units down to leave
// room for labels.
const TOP = 14
const FIELD_TOP = TOP + 6
const FIELD_BOTTOM = TOP + 118
const PW = 14
const SPARE_TRAY = 6
const LEFT = SPARE_TRAY + 24
const BAR = LEFT + 6 * PW + 6
const RIGHT = BAR + 6
const TRAY = RIGHT + 6 * PW + 4
const HOME_CENTRE = RIGHT + 3 * PW

/** Screen position of one of the reader's points (1 = next to the tray, bottom-right). */
function pip(n: number): { x: number; top: boolean } {
  if (n <= 6) return { x: RIGHT + (6 - n) * PW + PW / 2, top: false }
  if (n <= 12) return { x: LEFT + (12 - n) * PW + PW / 2, top: false }
  if (n <= 18) return { x: LEFT + (n - 13) * PW + PW / 2, top: true }
  return { x: RIGHT + (n - 19) * PW + PW / 2, top: true }
}

function stackY(top: boolean, i: number) {
  return top ? FIELD_TOP + 7 + i * 13 : FIELD_BOTTOM - 7 - i * 13
}

function MiniBoard({ children }: { children?: ReactNode }) {
  const points = Array.from({ length: 24 }, (_, i) => i + 1)
  return (
    <g>
      <rect x={0} y={TOP} width={240} height={124} rx={8} fill={WOOD.frame} />
      {[LEFT, RIGHT].map((x) => <rect key={x} x={x} y={FIELD_TOP} width={6 * PW} height={112} fill={WOOD.field} />)}
      <line x1={BAR} y1={TOP} x2={BAR} y2={TOP + 124} stroke="#1e1108" strokeWidth={1.5} />
      {[SPARE_TRAY, TRAY].map((x) => <rect key={x} x={x} y={FIELD_TOP} width={20} height={112} rx={3} fill={WOOD.tray} />)}
      {points.map((n) => {
        const { x, top } = pip(n)
        const base = top ? FIELD_TOP : FIELD_BOTTOM
        const apex = top ? FIELD_TOP + 46 : FIELD_BOTTOM - 46
        return <path key={n} d={`M ${x - 6} ${base} L ${x + 6} ${base} L ${x} ${apex} Z`}
          fill={n % 2 ? WOOD.dark : WOOD.light} />
      })}
      {children}
    </g>
  )
}

function Label({ x, y, children, anchor = 'middle' }: {
  x: number; y: number; children: ReactNode; anchor?: 'start' | 'middle' | 'end'
}) {
  return <text className="rule-label" x={x} y={y} textAnchor={anchor}>{children}</text>
}

/** Start top-right, travel counter-clockwise, finish in the home quarter and off into the tray. */
export function GoalArt() {
  return (
    <Svg width={240} height={152} label="Your checkers travel counter-clockwise from top-right to your home, bottom-right, then off">
      <MiniBoard>
        <rect x={RIGHT} y={FIELD_BOTTOM - 52} width={6 * PW} height={52} fill={BLUE} opacity={0.14} />
        <Checker x={pip(24).x} y={stackY(true, 0)} tone="ivory" />
        <Checker x={pip(24).x} y={stackY(true, 1)} tone="ivory" />
        <Arrow d={`M ${pip(24).x - 10} ${FIELD_TOP + 34} H ${LEFT + 16} Q ${LEFT + 8} ${FIELD_TOP + 34} ${LEFT + 8} ${FIELD_TOP + 44} V ${FIELD_BOTTOM - 40} Q ${LEFT + 8} ${FIELD_BOTTOM - 30} ${LEFT + 18} ${FIELD_BOTTOM - 30} H ${TRAY + 8}`} />
      </MiniBoard>
      <Label x={pip(24).x} y={10}>start</Label>
      <Label x={HOME_CENTRE} y={150}>your home</Label>
      <Label x={TRAY + 10} y={150}>off</Label>
    </Svg>
  )
}

/** One die each: the higher number starts. */
export function OpeningArt() {
  return (
    <Svg width={240} height={70} label="White throws 5, Black throws 2: White starts">
      <Die x={30} y={10} value={5} size={30} />
      <Label x={45} y={60}>White</Label>
      <text className="rule-big" x={92} y={32} textAnchor="middle">vs</text>
      <Die x={124} y={10} value={2} size={30} tone="charcoal" />
      <Label x={139} y={60}>Black</Label>
      <Arrow d="M 166 25 H 184" />
      <Checker x={204} y={25} tone="ivory" r={11} />
      <Label x={204} y={60}>White starts</Label>
    </Svg>
  )
}

/** Move a checker the number of points on each die. */
export function MoveArt() {
  const xs = Array.from({ length: 8 }, (_, i) => 22 + i * 28)
  return (
    <Svg width={240} height={96} label="With 3 and 4, one checker moves 3 points, then 4 more">
      <rect x={6} y={44} width={228} height={48} rx={4} fill={WOOD.field} />
      {xs.map((x, i) => (
        <path key={i} d={`M ${x - 11} 92 L ${x + 11} 92 L ${x} 50 Z`} fill={i % 2 ? WOOD.light : WOOD.dark} />
      ))}
      <Checker x={xs[0]} y={84} tone="ivory" r={8} />
      <circle cx={xs[7]} cy={84} r={8} fill="none" stroke={BLUE} strokeWidth={2} strokeDasharray="3 3" />
      <Arrow d={`M ${xs[0]} 70 Q ${(xs[0] + xs[3]) / 2} 34 ${xs[3]} 66`} />
      <Arrow d={`M ${xs[3]} 70 Q ${(xs[3] + xs[7]) / 2} 30 ${xs[7]} 66`} />
      <Die x={(xs[0] + xs[3]) / 2 - 10} y={4} value={3} size={20} />
      <Die x={(xs[3] + xs[7]) / 2 - 10} y={2} value={4} size={20} />
    </Svg>
  )
}

/** Doubles are played four times. */
export function DoublesArt() {
  return (
    <Svg width={240} height={56} label="A double 4 gives four moves of 4">
      <Die x={8} y={12} value={4} size={28} />
      <Die x={40} y={12} value={4} size={28} />
      <text className="rule-big" x={88} y={34} textAnchor="middle">=</text>
      {[0, 1, 2, 3].map((i) => <Die key={i} x={108 + i * 32} y={14} value={4} size={24} />)}
    </Svg>
  )
}

/** Empty, your own, or a single opponent (a hit) is fine; two or more opponents block. */
export function LandingArt() {
  const cases: { label: string; ok: boolean; stack: ('ivory' | 'charcoal')[] }[] = [
    { label: 'empty', ok: true, stack: [] },
    { label: 'yours', ok: true, stack: ['ivory', 'ivory'] },
    { label: 'hit!', ok: true, stack: ['charcoal'] },
    { label: 'blocked', ok: false, stack: ['charcoal', 'charcoal'] },
  ]
  return (
    <Svg width={240} height={112} label="You may land on an empty point, your own, or a single opponent; not two or more opponents">
      {cases.map((c, i) => {
        const x = 30 + i * 60
        return (
          <g key={c.label}>
            <rect x={x - 26} y={20} width={52} height={68} rx={4} fill={WOOD.field} />
            <path d={`M ${x - 15} 88 L ${x + 15} 88 L ${x} 34 Z`} fill={i % 2 ? WOOD.light : WOOD.dark} />
            {c.stack.map((tone, j) => <Checker key={j} x={x} y={79 - j * 16} tone={tone} r={7.5} />)}
            <Tick x={x} y={9} ok={c.ok} />
            <Label x={x} y={104}>{c.label}</Label>
          </g>
        )
      })}
    </Svg>
  )
}

/** A hit checker waits on the bar and must come back in through the opponent's home, top-right. */
export function BarArt() {
  return (
    <Svg width={240} height={152} label="A hit checker goes to the bar and must re-enter in the top-right quarter before anything else moves">
      <MiniBoard>
        <rect x={RIGHT} y={FIELD_TOP} width={6 * PW} height={52} fill={BLUE} opacity={0.16} />
        <Checker x={BAR} y={FIELD_TOP + 70} tone="ivory" glow />
        <Arrow d={`M ${BAR + 4} ${FIELD_TOP + 62} Q ${BAR + 20} ${FIELD_TOP + 34} ${pip(21).x} ${FIELD_TOP + 14}`} />
        <Checker x={pip(23).x} y={stackY(true, 0)} tone="charcoal" />
        <Checker x={pip(23).x} y={stackY(true, 1)} tone="charcoal" />
      </MiniBoard>
      <Label x={HOME_CENTRE} y={10}>come back in here</Label>
      <Label x={BAR} y={150}>the bar</Label>
    </Svg>
  )
}

/** With all 15 home, checkers leave the board into the tray. */
export function BearOffArt() {
  const home = [[1, 2], [2, 2], [3, 3], [4, 2], [5, 3], [6, 3]]
  return (
    <Svg width={240} height={152} label="When all your checkers are in your home quarter, move them off into the tray">
      <MiniBoard>
        <rect x={RIGHT} y={FIELD_BOTTOM - 52} width={6 * PW} height={52} fill={BLUE} opacity={0.14} />
        {home.flatMap(([n, count]) =>
          Array.from({ length: count }, (_, i) => (
            <Checker key={`${n}-${i}`} x={pip(n).x} y={stackY(false, i)} tone="ivory" />
          )),
        )}
        {[0, 1, 2].map((i) => (
          <rect key={i} x={TRAY + 3} y={FIELD_BOTTOM - 10 - i * 7} width={14} height={5} rx={1.5} fill="#f6eedb" />
        ))}
        <Arrow d={`M ${pip(3).x} ${FIELD_BOTTOM - 48} Q ${pip(1).x + 4} ${FIELD_BOTTOM - 76} ${TRAY + 10} ${FIELD_BOTTOM - 34}`} />
      </MiniBoard>
      <Label x={HOME_CENTRE} y={150}>all 15 home</Label>
      <Label x={TRAY + 10} y={150}>off</Label>
    </Svg>
  )
}

/** Use both dice if possible; otherwise the higher; otherwise pass. */
export function DiceRuleArt() {
  return (
    <Svg width={240} height={74} label="Use both dice if you can; if only one fits, the higher; if none, pass">
      <Die x={6} y={8} value={6} size={24} />
      <Die x={34} y={8} value={2} size={24} />
      <Label x={33} y={50}>both</Label>
      <Label x={33} y={64}>if you can</Label>
      <Die x={104} y={8} value={6} size={24} />
      <Die x={132} y={8} value={2} size={24} dim />
      <Label x={130} y={50}>else the</Label>
      <Label x={130} y={64}>higher</Label>
      <rect x={186} y={10} width={48} height={22} rx={11} fill={BLUE} />
      <text x={210} y={25} textAnchor="middle" className="rule-pill">Pass</text>
      <Label x={210} y={50}>if nothing</Label>
      <Label x={210} y={64}>moves</Label>
    </Svg>
  )
}

/** Roll, pick a glowing checker, pick a ring, confirm. */
export function ControlsArt() {
  return (
    <Svg width={240} height={60} label="Roll, click a glowing checker, click a ring, then Confirm">
      <rect x={4} y={12} width={44} height={22} rx={11} fill={BLUE} />
      <text x={26} y={27} textAnchor="middle" className="rule-pill">Roll</text>
      <Arrow d="M 52 23 H 66" />
      <Checker x={84} y={23} tone="ivory" r={9} glow />
      <Arrow d="M 100 23 H 114" />
      <circle cx={132} cy={23} r={9} fill="rgba(255,248,225,0.4)" stroke={BLUE} strokeWidth={2} strokeDasharray="3 3" />
      <Arrow d="M 148 23 H 162" />
      <rect x={168} y={12} width={68} height={22} rx={11} fill={BLUE} />
      <text x={202} y={27} textAnchor="middle" className="rule-pill">Confirm</text>
      <Label x={26} y={52}>dice</Label>
      <Label x={84} y={52}>checker</Label>
      <Label x={132} y={52}>where</Label>
      <Label x={202} y={52}>done</Label>
    </Svg>
  )
}
