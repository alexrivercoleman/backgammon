# Tavli Portes

A web version of Tavli Portes (Greek backgammon) for two players sharing one screen.

## Layout

| Path                 | What                                                     |
|----------------------|----------------------------------------------------------|
| `backend/`           | Java 25 / Spring Boot server, including the rules engine |
| `frontend/`          | React + TypeScript + Vite client                         |
| `k8s/`               | Kubernetes manifests (k3d)                               |
| `docker-compose.yml` | Local development services (Postgres)                    |

The rules engine (`backend/src/main/java/com/tavli/engine`) is plain Java with no framework imports. The
server is authoritative: it rolls the dice, sends every legal way to play the roll, and validates each
submitted turn.

## Development

Requires Java 25, Node 24 and Docker.

```sh
make db-up           # start Postgres (docker compose)
make backend-run     # start the server on :8080; Flyway migrates the schema on startup
make frontend-dev    # start the Vite dev server on :5173 (proxies /api to :8080)

make test            # backend and frontend unit tests (no Docker needed)
make verify          # backend unit + integration tests (Testcontainers Postgres; Docker must be running)
```

Then open http://localhost:5173. Each page load starts a new game.

## Playing

Two players share the screen. The board is always drawn from the point of view of the player to move:
their home is bottom-right next to the bear-off tray, and the board flips when the turn passes.

1. Click a checker with a gold ring to pick it up; dashed rings show where it can go (red if it hits a blot,
   the tray if it can bear off).
2. Click a destination. Repeat for each die; played dice are dimmed. **Undo** takes back one move.
3. **Confirm** once the turn is complete, or **Pass** when no move is possible.

The UI only follows the turn graph the server sends; it never decides what is legal. When bearing off could
use either die, a click uses the lower one so the higher stays available (undo to choose otherwise).

Frontend layout: `src/api` (typed client), `src/game` (turn walking and the `useGame` hook), `src/board`
(SVG board, geometry and flip animation), `src/components` (panel, dice, opening banner, game over).

## API

All endpoints are under `/api/games`. Errors are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem
details.

| Method | Path                    | Body                       | Result                                           |
|--------|-------------------------|----------------------------|--------------------------------------------------|
| POST   | `/api/games`            |                            | `201` new game (opening throws already rolled)   |
| GET    | `/api/games/{id}`       |                            | `200` game                                       |
| POST   | `/api/games/{id}/turns` | `{"turnNumber", "moves"}`  | `200` game after the turn; `400`, `404`, `409` stale or finished, `422` illegal |
| GET    | `/api/games/{id}/log`   |                            | `200` every opening throw, roll and turn in order |

- **Board**: `points` has 24 signed counts, index 0 = point 1; positive is WHITE, negative is BLACK. WHITE moves
  24 → 1 (home 1–6), BLACK moves 1 → 24 (home 19–24). `bar` and `off` count checkers per player.
- **Moves**: `{"from": 13 | "bar", "to": 7 | "off", "die": 6}`.
- **Turn graph**: while a game is in progress, `turn.nodes` lists every position reachable this turn. Node 0
  is the current position; each node has its `board`, the `unusedDice`, whether the turn may be submitted
  there (`finished`), and its legal `moves`, each pointing to the `next` node. Different orders of the same
  moves lead to the same node. Submit the moves taken from node 0 to any finished node; when `mustPass` is
  true, submit `[]`.
- **turnNumber**: send the `turnNumber` from the game you are playing; a mismatch (e.g. a double submit) is
  rejected with `409`.

## Rules

Standard Portes. If no move is possible the player passes. A turn must use as many dice as possible, and the
higher die if only one can be used; a turn that bears off the last checker wins even if a die is left over.
The doubling cube and gammons are not played.
