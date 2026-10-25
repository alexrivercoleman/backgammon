// The game being played lives in the address bar as ?game=<id>, so a refresh reopens it.

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/** The game id in a query string, or null if there is none or it isn't a game id. */
export function linkedGameId(search: string): string | null {
  const id = new URLSearchParams(search).get('game')
  return id && UUID.test(id) ? id : null
}

/** `search` with its game id set to `id`, other parameters kept. */
export function linkTo(search: string, id: string): string {
  const params = new URLSearchParams(search)
  params.set('game', id)
  return `?${params}`
}
