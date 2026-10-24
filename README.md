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
make frontend-dev    # start the Vite dev server

make test            # unit tests (no Docker needed)
make verify          # unit + integration tests (Testcontainers Postgres; Docker must be running)
```

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
