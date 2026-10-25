.PHONY: help test verify backend-test backend-verify frontend-test backend-build backend-run frontend-install frontend-build frontend-dev db-up db-down compose-up compose-down

# Host port the containerised app is published on.
APP_PORT ?= 8088
export APP_PORT

help:
	@echo "Targets:"
	@echo "  test             Run backend and frontend unit tests (no Docker needed)"
	@echo "  verify           Run unit and integration tests (needs Docker)"
	@echo "  backend-build    Build the backend jar"
	@echo "  backend-run      Run the backend locally"
	@echo "  frontend-build   Build the frontend bundle"
	@echo "  frontend-dev     Run the Vite dev server"
	@echo "  db-up / db-down  Start / stop local Postgres via docker compose"
	@echo "  compose-up       Build and run the whole stack with docker compose on :$(APP_PORT)"
	@echo "  compose-down     Stop the docker compose stack"

test: backend-test frontend-test

verify: backend-verify

backend-test:
	cd backend && ./mvnw -B test

backend-verify:
	cd backend && ./mvnw -B verify

backend-build:
	cd backend && ./mvnw -B package

backend-run:
	cd backend && ./mvnw spring-boot:run

# Reinstall only when the lockfile changes.
frontend/node_modules: frontend/package-lock.json
	cd frontend && npm ci
	@touch frontend/node_modules

frontend-install: frontend/node_modules

frontend-test: frontend/node_modules
	cd frontend && npm test

frontend-build: frontend/node_modules
	cd frontend && npm run build

frontend-dev: frontend/node_modules
	cd frontend && npm run dev

db-up:
	docker compose up -d postgres

db-down:
	docker compose stop postgres

compose-up:
	docker compose up -d --build --wait
	@echo "Tavli is running at http://localhost:$(APP_PORT)"

compose-down:
	docker compose down
