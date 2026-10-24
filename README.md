# Tavli Portes

A web version of Tavli Portes (Greek backgammon) for two players sharing one screen.

## Layout

| Path                 | What                                                   |
|----------------------|--------------------------------------------------------|
| `backend/`           | Java 25 / Spring Boot server, including the rules engine |
| `frontend/`          | React + TypeScript + Vite client                       |
| `k8s/`               | Kubernetes manifests (k3d)                             |
| `docker-compose.yml` | Local development services                             |

## Development

```sh
make test            # run backend tests
make backend-run     # start the server on :8080
make frontend-dev    # start the Vite dev server
```

_More to come._
