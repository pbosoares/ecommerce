FROM node:22-alpine AS build
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/index.html frontend/vite.config.js ./
COPY frontend/public ./public
COPY frontend/src ./src
RUN npm run build

FROM caddy:2-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /build/dist /srv
