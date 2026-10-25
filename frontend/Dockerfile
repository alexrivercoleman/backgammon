# syntax=docker/dockerfile:1

# Build: install from the lockfile (npm cache in a BuildKit mount), type-check and bundle.
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Run: nginx serves the bundle and proxies /api to the backend named by BACKEND_URL.
FROM nginx:1.30-alpine
ENV BACKEND_URL=http://backend:8080
# Makes the image's entrypoint export NGINX_LOCAL_RESOLVERS for the config template.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
