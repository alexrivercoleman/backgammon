#!/usr/bin/env sh
# Builds the images, creates the k3d cluster if needed, deploys k8s/ and waits until the app is ready.
# Safe to re-run: a rebuilt image rolls its Deployment, an unchanged one leaves the pods alone.
set -eu

CLUSTER=${CLUSTER:-tavli}
APP_PORT=${APP_PORT:-8088}
CONTEXT="k3d-$CLUSTER"

for tool in docker k3d kubectl; do
  command -v "$tool" >/dev/null 2>&1 || { echo "make up needs '$tool' on the PATH" >&2; exit 1; }
done
docker info >/dev/null 2>&1 || { echo "Docker is not running" >&2; exit 1; }

if ! k3d cluster get "$CLUSTER" >/dev/null 2>&1; then
  echo "Creating k3d cluster '$CLUSTER' with the app on port $APP_PORT"
  k3d cluster create "$CLUSTER" --port "$APP_PORT:80@loadbalancer" --wait
fi

# Without provenance/SBOM attestations (which carry a timestamp), an unchanged rebuild keeps the same image ID.
docker build --provenance=false --sbom=false -t tavli-backend:dev backend
docker build --provenance=false --sbom=false -t tavli-frontend:dev frontend

# Tag each image with its content ID; the Deployments are pointed at that tag below.
image_tag() {
  docker image inspect --format '{{.Id}}' "tavli-$1:dev" | sed 's/^sha256://' | cut -c1-12
}
backend_tag=$(image_tag backend)
frontend_tag=$(image_tag frontend)
docker tag tavli-backend:dev "tavli-backend:$backend_tag"
docker tag tavli-frontend:dev "tavli-frontend:$frontend_tag"
k3d image import -c "$CLUSTER" \
  tavli-backend:dev "tavli-backend:$backend_tag" tavli-frontend:dev "tavli-frontend:$frontend_tag"

kubectl --context "$CONTEXT" apply -k k8s
kubectl --context "$CONTEXT" -n tavli set image deployment/backend "backend=tavli-backend:$backend_tag"
kubectl --context "$CONTEXT" -n tavli set image deployment/frontend "frontend=tavli-frontend:$frontend_tag"

kubectl --context "$CONTEXT" -n tavli rollout status statefulset/postgres --timeout=180s
kubectl --context "$CONTEXT" -n tavli rollout status deployment/backend --timeout=300s
kubectl --context "$CONTEXT" -n tavli rollout status deployment/frontend --timeout=120s

echo "Tavli is running at http://localhost:$APP_PORT"
