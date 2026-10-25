import type { GameView, LogEntry, Move, ProblemDetail } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.detail ?? problem?.title ?? `Request failed with status ${status}`)
    this.status = status
    this.problem = problem
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    let problem: ProblemDetail | null = null
    try {
      problem = (await response.json()) as ProblemDetail
    } catch {
      // Not a problem-details body; report the status alone.
    }
    throw new ApiError(response.status, problem)
  }
  return (await response.json()) as T
}

export function createGame(): Promise<GameView> {
  return request('/api/games', { method: 'POST' })
}

export function getGame(id: string): Promise<GameView> {
  return request(`/api/games/${id}`)
}

export function submitTurn(id: string, turnNumber: number, moves: Move[]): Promise<GameView> {
  return request(`/api/games/${id}/turns`, {
    method: 'POST',
    body: JSON.stringify({ turnNumber, moves }),
  })
}

export function getLog(id: string): Promise<LogEntry[]> {
  return request(`/api/games/${id}/log`)
}
