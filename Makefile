.PHONY: help test backend-test backend-build backend-run frontend-install frontend-build frontend-dev db-up db-down

help:
	@echo "Targets:"
	@echo "  test             Run all tests"
	@echo "  backend-build    Build the backend jar"
	@echo "  backend-run      Run the backend locally"
	@echo "  frontend-build   Build the frontend bundle"
	@echo "  frontend-dev     Run the Vite dev server"
	@echo "  db-up / db-down  Start / stop local Postgres via docker compose"

test: backend-test

backend-test:
	cd backend && ./mvnw -B test

backend-build:
	cd backend && ./mvnw -B package

backend-run:
	cd backend && ./mvnw spring-boot:run

frontend-install:
	cd frontend && npm ci

frontend-build: frontend-install
	cd frontend && npm run build

frontend-dev:
	cd frontend && npm run dev

db-up:
	docker compose up -d postgres

db-down:
	docker compose down
